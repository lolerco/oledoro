package com.lolerco.oledoro.habits

import com.lolerco.oledoro.data.GruvboxColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitStreakCalculatorTest {

    private val baseDate = LocalDate.of(2026, 10, 30)

    @Test
    fun `empty completions yields zero streaks`() {
        val habit = Habit(id = "1", name = "Water", completedDates = emptySet())
        val stats = HabitStreakCalculator.calculateStats(habit, baseDate)

        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.bestStreak)
        assertEquals(0, stats.totalCompletions)
        assertEquals(0, stats.weeklyRatePercent)
    }

    @Test
    fun `completed today gives active streak starting from today`() {
        val completed = setOf(
            baseDate.toString(),
            baseDate.minusDays(1).toString(),
            baseDate.minusDays(2).toString()
        )
        val streak = HabitStreakCalculator.calculateCurrentStreak(completed, baseDate)
        assertEquals(3, streak)
    }

    @Test
    fun `completed yesterday but not yet today maintains active streak in uhabits style`() {
        val completed = setOf(
            baseDate.minusDays(1).toString(),
            baseDate.minusDays(2).toString(),
            baseDate.minusDays(3).toString()
        )
        // Today is not checked, but yesterday was, so the streak is 3 and not broken
        val streak = HabitStreakCalculator.calculateCurrentStreak(completed, baseDate)
        assertEquals(3, streak)
    }

    @Test
    fun `missed yesterday and today breaks the current streak to zero`() {
        val completed = setOf(
            baseDate.minusDays(2).toString(),
            baseDate.minusDays(3).toString()
        )
        val streak = HabitStreakCalculator.calculateCurrentStreak(completed, baseDate)
        assertEquals(0, streak)
    }

    @Test
    fun `best streak finds the longest historical sequence`() {
        // Run 1: 5 days
        // Run 2: 8 days
        // Run 3: 3 days
        val dates = mutableSetOf<String>()
        for (i in 40 downTo 36) dates.add(baseDate.minusDays(i.toLong()).toString())
        for (i in 25 downTo 18) dates.add(baseDate.minusDays(i.toLong()).toString())
        for (i in 3 downTo 1) dates.add(baseDate.minusDays(i.toLong()).toString())

        val best = HabitStreakCalculator.calculateBestStreak(dates)
        assertEquals(8, best)

        val current = HabitStreakCalculator.calculateCurrentStreak(dates, baseDate)
        assertEquals(3, current)
    }

    @Test
    fun `getPastWeek returns exactly 7 days ending on today`() {
        val habit = Habit(
            id = "h1",
            name = "Read",
            completedDates = setOf(baseDate.toString(), baseDate.minusDays(2).toString())
        )
        val week = HabitStreakCalculator.getPastWeek(habit, baseDate)

        assertEquals(7, week.size)
        assertEquals(baseDate.minusDays(6), week.first().date)
        assertEquals(baseDate, week.last().date)
        assertTrue(week.last().isToday)
        assertTrue(week.last().isCompleted)
        assertFalse(week[week.size - 2].isCompleted) // yesterday not completed
        assertTrue(week[week.size - 3].isCompleted) // 2 days ago completed
    }

    @Test
    fun `calculateStats calculates accurate completion percentages`() {
        // 4 completions in past 7 days
        val completed = setOf(
            baseDate.toString(),
            baseDate.minusDays(1).toString(),
            baseDate.minusDays(3).toString(),
            baseDate.minusDays(5).toString()
        )
        val habit = Habit(
            id = "h2",
            name = "Meditate",
            createdAt = baseDate.minusDays(6).toString(),
            completedDates = completed
        )
        val stats = HabitStreakCalculator.calculateStats(habit, baseDate)

        assertEquals(2, stats.currentStreak)
        assertEquals(4, stats.totalCompletions)
        // 4 / 7 = 57%
        assertEquals(57, stats.weeklyRatePercent)
    }
}
