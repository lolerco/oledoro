package com.lolerco.oledoro.habits

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Core streak recognition and statistical analysis matching uhabits (Loop Habit Tracker).
 * Streaks and completion rates adapt to the habit's frequency target (targetDaysPerWeek).
 *
 * For Daily habits (targetDaysPerWeek == 7):
 * - Streaks count consecutive days.
 * - Completed days increment the streak by 1.
 * - Skipped days prolong the streak without incrementing it.
 * - If today is not checked (off), but yesterday was active, the streak is alive up through yesterday.
 * - An off day breaks the streak.
 *
 * For Weekly habits (targetDaysPerWeek < 7):
 * - Streaks count consecutive weeks meeting or exceeding the target.
 * - Current week: if target is already met, the current week increments the streak.
 *   If not yet met, but remaining days in the week can still reach the target, the prior streak is kept alive.
 *   If mathematically impossible to reach the target with remaining days, the streak is broken (0).
 * - Historical weeks: each week where completed >= target increments streak; skipped weeks (completed + skipped >= target)
 *   prolong the streak without incrementing.
 */
object HabitStreakCalculator {

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Calculates the current active streak length.
     * When targetDaysPerWeek == 7, streak is measured in days.
     * When targetDaysPerWeek < 7, streak is measured in weeks where the target was achieved.
     */
    fun calculateCurrentStreak(
        completedDates: Set<String>,
        skippedDates: Set<String> = emptySet(),
        today: LocalDate = LocalDate.now(),
        targetDaysPerWeek: Int = 7
    ): Int {
        val completed = completedDates.mapNotNull { parseDateOrNull(it) }.toSet()
        val skipped = skippedDates.mapNotNull { parseDateOrNull(it) }.toSet()
        if (completed.isEmpty()) return 0

        val target = targetDaysPerWeek.coerceIn(1, 7)
        return if (target == 7) {
            calculateDailyCurrentStreak(completed, skipped, today)
        } else {
            calculateWeeklyCurrentStreak(completed, skipped, today, target)
        }
    }

    /**
     * Overload taking Habit directly.
     */
    fun calculateCurrentStreak(habit: Habit, today: LocalDate = LocalDate.now()): Int {
        return calculateCurrentStreak(
            completedDates = habit.completedDates,
            skippedDates = habit.skippedDates,
            today = today,
            targetDaysPerWeek = habit.targetDaysPerWeek
        )
    }

    private fun calculateDailyCurrentStreak(
        completed: Set<LocalDate>,
        skipped: Set<LocalDate>,
        today: LocalDate
    ): Int {
        val yesterday = today.minusDays(1)

        var currentCheckDate = when {
            completed.contains(today) || skipped.contains(today) -> today
            completed.contains(yesterday) || skipped.contains(yesterday) -> yesterday
            else -> return 0
        }

        var streak = 0
        while (true) {
            when {
                completed.contains(currentCheckDate) -> {
                    streak++
                    currentCheckDate = currentCheckDate.minusDays(1)
                }
                skipped.contains(currentCheckDate) -> {
                    // Skipped prolongs the streak without incrementing it
                    currentCheckDate = currentCheckDate.minusDays(1)
                }
                else -> break
            }
        }
        return streak
    }

    private fun calculateWeeklyCurrentStreak(
        completed: Set<LocalDate>,
        skipped: Set<LocalDate>,
        today: LocalDate,
        target: Int
    ): Int {
        val currentMonday = today.with(DayOfWeek.MONDAY)
        val daysPassedThisWeek = ChronoUnit.DAYS.between(currentMonday, today).toInt() + 1
        val daysRemainingThisWeek = 7 - daysPassedThisWeek

        val doneThisWeek = getWeekCount(currentMonday, completed)
        val skippedThisWeek = getWeekCount(currentMonday, skipped)

        val currentWeekMet = doneThisWeek >= target
        val currentWeekViable = doneThisWeek + skippedThisWeek + daysRemainingThisWeek >= target

        if (!currentWeekMet && !currentWeekViable) {
            // Cannot meet the weekly target anymore this week: streak is broken
            return 0
        }

        var streak = if (currentWeekMet) 1 else 0
        var checkMonday = currentMonday.minusWeeks(1)

        while (true) {
            val done = getWeekCount(checkMonday, completed)
            val skip = getWeekCount(checkMonday, skipped)
            if (done >= target) {
                streak++
                checkMonday = checkMonday.minusWeeks(1)
            } else if (done + skip >= target) {
                // Skipped week prolongs the streak without incrementing
                checkMonday = checkMonday.minusWeeks(1)
            } else {
                break
            }
        }
        return streak
    }

    /**
     * Calculates the best (longest) streak across the habit's entire history.
     */
    fun calculateBestStreak(
        completedDates: Set<String>,
        skippedDates: Set<String> = emptySet(),
        targetDaysPerWeek: Int = 7
    ): Int {
        val completed = completedDates.mapNotNull { parseDateOrNull(it) }.toSet()
        val skipped = skippedDates.mapNotNull { parseDateOrNull(it) }.toSet()
        if (completed.isEmpty()) return 0

        val target = targetDaysPerWeek.coerceIn(1, 7)
        return if (target == 7) {
            calculateDailyBestStreak(completed, skipped)
        } else {
            calculateWeeklyBestStreak(completed, skipped, target)
        }
    }

    /**
     * Overload taking Habit directly.
     */
    fun calculateBestStreak(habit: Habit): Int {
        return calculateBestStreak(
            completedDates = habit.completedDates,
            skippedDates = habit.skippedDates,
            targetDaysPerWeek = habit.targetDaysPerWeek
        )
    }

    private fun calculateDailyBestStreak(
        completed: Set<LocalDate>,
        skipped: Set<LocalDate>
    ): Int {
        val allActive = (completed + skipped).sorted()
        if (allActive.isEmpty()) return 0

        var bestStreak = 0
        var currentStreak = 0
        var prevDate: LocalDate? = null

        for (date in allActive) {
            if (prevDate != null && date != prevDate.plusDays(1)) {
                currentStreak = 0
            }
            if (completed.contains(date)) {
                currentStreak++
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak
                }
            }
            prevDate = date
        }

        return bestStreak
    }

    private fun calculateWeeklyBestStreak(
        completed: Set<LocalDate>,
        skipped: Set<LocalDate>,
        target: Int
    ): Int {
        val allActive = (completed + skipped)
        if (allActive.isEmpty()) return 0

        val earliest = allActive.minOrNull() ?: return 0
        val latest = allActive.maxOrNull() ?: return 0

        var bestStreak = 0
        var currentStreak = 0
        var weekMonday = earliest.with(DayOfWeek.MONDAY)
        val endMonday = latest.with(DayOfWeek.MONDAY)

        while (!weekMonday.isAfter(endMonday)) {
            val done = getWeekCount(weekMonday, completed)
            val skip = getWeekCount(weekMonday, skipped)

            if (done >= target) {
                currentStreak++
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak
                }
            } else if (done + skip >= target) {
                // Prolongs streak without incrementing
            } else {
                currentStreak = 0
            }
            weekMonday = weekMonday.plusWeeks(1)
        }

        return bestStreak
    }

    /**
     * Computes full statistics mirroring Loop Habit Tracker (uhabits).
     * Percentages are calculated relative to the habit's frequency target.
     */
    fun calculateStats(habit: Habit, today: LocalDate = LocalDate.now()): HabitStats {
        val currentStreak = calculateCurrentStreak(
            completedDates = habit.completedDates,
            skippedDates = habit.skippedDates,
            today = today,
            targetDaysPerWeek = habit.targetDaysPerWeek
        )
        val historicalBest = calculateBestStreak(
            completedDates = habit.completedDates,
            skippedDates = habit.skippedDates,
            targetDaysPerWeek = habit.targetDaysPerWeek
        )
        val bestStreak = maxOf(currentStreak, historicalBest)
        val totalCompletions = habit.completedDates.size
        val target = habit.targetDaysPerWeek.coerceIn(1, 7)

        // Past 7 days rate (relative to weekly target)
        var past7Count = 0
        for (i in 0 until 7) {
            val d = today.minusDays(i.toLong()).toString()
            if (habit.completedDates.contains(d)) {
                past7Count++
            }
        }
        val weeklyRatePercent = ((past7Count.toDouble() / target) * 100).roundToInt().coerceIn(0, 100)

        // Past 30 days rate (relative to target frequency)
        var past30Count = 0
        for (i in 0 until 30) {
            val d = today.minusDays(i.toLong()).toString()
            if (habit.completedDates.contains(d)) {
                past30Count++
            }
        }
        val expected30 = (target / 7.0) * 30.0
        val monthlyRatePercent = ((past30Count / expected30) * 100).roundToInt().coerceIn(0, 100)

        // All time rate (relative to target frequency)
        val createdDate = parseDateOrNull(habit.createdAt) ?: today
        val daysSinceCreation = (ChronoUnit.DAYS.between(createdDate, today) + 1).coerceAtLeast(1)
        val expectedAllTime = (target / 7.0) * daysSinceCreation
        val allTimeRatePercent = ((totalCompletions.toDouble() / expectedAllTime) * 100)
            .roundToInt()
            .coerceIn(0, 100)

        val streakUnit = if (habit.targetDaysPerWeek == 7) "days" else "weeks"

        return HabitStats(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalCompletions = totalCompletions,
            weeklyRatePercent = weeklyRatePercent,
            monthlyRatePercent = monthlyRatePercent,
            allTimeRatePercent = allTimeRatePercent,
            streakUnit = streakUnit
        )
    }

    /**
     * Generates the past 4 days (3 days ago through today) for the compact habit row streak view.
     */
    fun getPast4Days(habit: Habit, today: LocalDate = LocalDate.now()): List<DayCheck> {
        val result = mutableListOf<DayCheck>()
        for (offset in 3 downTo 0) {
            val date = today.minusDays(offset.toLong())
            val dateStr = date.toString()
            result.add(
                DayCheck(
                    date = date,
                    dateString = dateStr,
                    dayInitial = formatDayOfWeekInitial(date.dayOfWeek),
                    dayNumber = date.dayOfMonth,
                    isCompleted = habit.completedDates.contains(dateStr),
                    isToday = (offset == 0),
                    isSkipped = habit.skippedDates.contains(dateStr)
                )
            )
        }
        return result
    }

    /**
     * Generates the 7 days of the past week (6 days ago through today).
     */
    fun getPastWeek(habit: Habit, today: LocalDate = LocalDate.now()): List<DayCheck> {
        val result = mutableListOf<DayCheck>()
        for (offset in 6 downTo 0) {
            val date = today.minusDays(offset.toLong())
            val dateStr = date.toString()
            result.add(
                DayCheck(
                    date = date,
                    dateString = dateStr,
                    dayInitial = formatDayOfWeekInitial(date.dayOfWeek),
                    dayNumber = date.dayOfMonth,
                    isCompleted = habit.completedDates.contains(dateStr),
                    isToday = (offset == 0),
                    isSkipped = habit.skippedDates.contains(dateStr)
                )
            )
        }
        return result
    }

    /**
     * Generates the past 30 days for detailed calendar history grid.
     */
    fun getPast30Days(habit: Habit, today: LocalDate = LocalDate.now()): List<DayCheck> {
        val result = mutableListOf<DayCheck>()
        for (offset in 29 downTo 0) {
            val date = today.minusDays(offset.toLong())
            val dateStr = date.toString()
            result.add(
                DayCheck(
                    date = date,
                    dateString = dateStr,
                    dayInitial = formatDayOfWeekInitial(date.dayOfWeek),
                    dayNumber = date.dayOfMonth,
                    isCompleted = habit.completedDates.contains(dateStr),
                    isToday = (offset == 0),
                    isSkipped = habit.skippedDates.contains(dateStr)
                )
            )
        }
        return result
    }

    private fun getWeekCount(monday: LocalDate, dates: Set<LocalDate>): Int {
        var count = 0
        for (i in 0 until 7) {
            if (dates.contains(monday.plusDays(i.toLong()))) count++
        }
        return count
    }

    private fun formatDayOfWeekInitial(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "M"
            DayOfWeek.TUESDAY -> "T"
            DayOfWeek.WEDNESDAY -> "W"
            DayOfWeek.THURSDAY -> "T"
            DayOfWeek.FRIDAY -> "F"
            DayOfWeek.SATURDAY -> "S"
            DayOfWeek.SUNDAY -> "S"
        }
    }

    private fun parseDateOrNull(str: String): LocalDate? {
        return try {
            LocalDate.parse(str, DATE_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }
}