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
     * Calculates the current active streak length.
     * In uhabits, if today is not yet checked, but yesterday was checked,
     * the streak is still considered alive and counted up to yesterday.
     */
    fun calculateCurrentStreak(completedDates: Set<String>, today: LocalDate = LocalDate.now()): Int {
        if (completedDates.isEmpty()) return 0

        val parsedDates = completedDates.mapNotNull { parseDateOrNull(it) }.toSet()
        val yesterday = today.minusDays(1)

        var currentCheckDate = when {
            parsedDates.contains(today) -> today
            parsedDates.contains(yesterday) -> yesterday
            else -> return 0
        }

        var streak = 0
        while (parsedDates.contains(currentCheckDate)) {
            streak++
            currentCheckDate = currentCheckDate.minusDays(1)
        }
        return streak
    }

    /**
     * Calculates the best (longest) streak across the habit's entire history.
     */
    fun calculateBestStreak(completedDates: Set<String>): Int {
        if (completedDates.isEmpty()) return 0

        val sortedDates = completedDates.mapNotNull { parseDateOrNull(it) }.sorted()
        if (sortedDates.isEmpty()) return 0

        var bestStreak = 1
        var currentRun = 1

        for (i in 1 until sortedDates.size) {
            val prev = sortedDates[i - 1]
            val curr = sortedDates[i]

            if (curr == prev.plusDays(1)) {
                currentRun++
                if (currentRun > bestStreak) {
                    bestStreak = currentRun
                }
            } else if (curr != prev) {
                currentRun = 1
            }
        }

        return bestStreak
    }

    /**
     * Computes full statistics mirroring Loop Habit Tracker (uhabits).
     */
    fun calculateStats(habit: Habit, today: LocalDate = LocalDate.now()): HabitStats {
        val currentStreak = calculateCurrentStreak(habit.completedDates, today)
        val historicalBest = calculateBestStreak(habit.completedDates)
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
                    isToday = (offset == 0)
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
                    isToday = (offset == 0)
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
