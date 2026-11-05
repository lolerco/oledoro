package com.lolerco.oledoro.habits

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Core streak recognition and statistical analysis matching uhabits (Loop Habit Tracker).
 */
object HabitStreakCalculator {

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Calculates the current active streak length (counting consecutive days).
     * - Completed days increment the streak by 1.
     * - Skipped days prolong the streak without incrementing it.
     * - If today is not checked (off), but yesterday was active (completed or skipped),
     *   the streak is alive up through yesterday.
     * - An off day breaks the streak.
     */
    fun calculateCurrentStreak(
        completedDates: Set<String>,
        skippedDates: Set<String> = emptySet(),
        today: LocalDate = LocalDate.now()
    ): Int {
        val completed = completedDates.mapNotNull { parseDateOrNull(it) }.toSet()
        val skipped = skippedDates.mapNotNull { parseDateOrNull(it) }.toSet()
        if (completed.isEmpty()) return 0

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

    /**
     * Calculates the best (longest) streak across the habit's entire history.
     */
    fun calculateBestStreak(
        completedDates: Set<String>,
        skippedDates: Set<String> = emptySet()
    ): Int {
        val completed = completedDates.mapNotNull { parseDateOrNull(it) }.toSet()
        val skipped = skippedDates.mapNotNull { parseDateOrNull(it) }.toSet()
        if (completed.isEmpty()) return 0

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

    /**
     * Computes full statistics mirroring Loop Habit Tracker (uhabits).
     */
    fun calculateStats(habit: Habit, today: LocalDate = LocalDate.now()): HabitStats {
        val currentStreak = calculateCurrentStreak(habit.completedDates, habit.skippedDates, today)
        val historicalBest = calculateBestStreak(habit.completedDates, habit.skippedDates)
        val bestStreak = maxOf(currentStreak, historicalBest)
        val totalCompletions = habit.completedDates.size

        // Past 7 days rate
        var past7Count = 0
        for (i in 0 until 7) {
            val d = today.minusDays(i.toLong()).toString()
            if (habit.completedDates.contains(d)) {
                past7Count++
            }
        }
        val weeklyRatePercent = ((past7Count / 7.0) * 100).roundToInt().coerceIn(0, 100)

        // Past 30 days rate
        var past30Count = 0
        for (i in 0 until 30) {
            val d = today.minusDays(i.toLong()).toString()
            if (habit.completedDates.contains(d)) {
                past30Count++
            }
        }
        val monthlyRatePercent = ((past30Count / 30.0) * 100).roundToInt().coerceIn(0, 100)

        // All time rate
        val createdDate = parseDateOrNull(habit.createdAt) ?: today
        val daysSinceCreation = (ChronoUnit.DAYS.between(createdDate, today) + 1).coerceAtLeast(1)
        val allTimeRatePercent = ((totalCompletions.toDouble() / daysSinceCreation) * 100)
            .roundToInt()
            .coerceIn(0, 100)

        return HabitStats(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalCompletions = totalCompletions,
            weeklyRatePercent = weeklyRatePercent,
            monthlyRatePercent = monthlyRatePercent,
            allTimeRatePercent = allTimeRatePercent
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
