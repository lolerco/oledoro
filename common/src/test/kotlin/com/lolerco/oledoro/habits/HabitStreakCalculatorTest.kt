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
        val streak = HabitStreakCalculator.calculateCurrentStreak(completed, today = baseDate)
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
        val streak = HabitStreakCalculator.calculateCurrentStreak(completed, today = baseDate)
        assertEquals(3, streak)
    }

    @Test
    fun `missed yesterday and today breaks the current streak to zero`() {
        val completed = setOf(
            baseDate.minusDays(2).toString(),
            baseDate.minusDays(3).toString()
        )
        val streak = HabitStreakCalculator.calculateCurrentStreak(completed, today = baseDate)
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

        val current = HabitStreakCalculator.calculateCurrentStreak(dates, today = baseDate)
        assertEquals(3, current)
    }

    @Test
    fun `skipped days prolong current streak without incrementing it`() {
        // Today = completed, yesterday = skipped, 2 days ago = completed -> streak = 2
        val completed = setOf(baseDate.toString(), baseDate.minusDays(2).toString())
        val skipped = setOf(baseDate.minusDays(1).toString())
        val streak = HabitStreakCalculator.calculateCurrentStreak(completed, skipped, baseDate)
        assertEquals(2, streak)
    }

    @Test
    fun `getPast4Days returns exactly 4 days ending on today`() {
        val habit = Habit(
            id = "h1",
            name = "Read",
            completedDates = setOf(baseDate.toString(), baseDate.minusDays(2).toString()),
            skippedDates = setOf(baseDate.minusDays(1).toString())
        )
        val past4 = HabitStreakCalculator.getPast4Days(habit, baseDate)

        assertEquals(4, past4.size)
        assertEquals(baseDate.minusDays(3), past4.first().date)
        assertEquals(baseDate, past4.last().date)
        assertTrue(past4.last().isToday)
        assertTrue(past4.last().isCompleted)
        assertTrue(past4[past4.size - 2].isSkipped)
        assertTrue(past4[past4.size - 3].isCompleted)
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

    @Test
    fun `weekly habit streak counts weeks reaching targetDaysPerWeek`() {
        // Target: 3 times a week
        // baseDate is Friday Oct 30, 2026. Monday was Oct 26.
        // Week 1 (Oct 26 - Nov 1): Completed Oct 26, 28, 30 -> 3 completions (target reached!)
        // Week 2 (Oct 19 - Oct 25): Completed Oct 19, 21, 23 -> 3 completions (target reached!)
        // Week 3 (Oct 12 - Oct 18): Completed Oct 12, 14, 16 -> 3 completions (target reached!)
        // Week 4 (Oct 5 - Oct 11): Completed Oct 5 -> 1 completion (failed, streak breaks)
        val completed = setOf(
            LocalDate.of(2026, 10, 30).toString(),
            LocalDate.of(2026, 10, 28).toString(),
            LocalDate.of(2026, 10, 26).toString(),
            LocalDate.of(2026, 10, 23).toString(),
            LocalDate.of(2026, 10, 21).toString(),
            LocalDate.of(2026, 10, 19).toString(),
            LocalDate.of(2026, 10, 16).toString(),
            LocalDate.of(2026, 10, 14).toString(),
            LocalDate.of(2026, 10, 12).toString(),
            LocalDate.of(2026, 10, 5).toString()
        )
        val streak = HabitStreakCalculator.calculateCurrentStreak(
            completedDates = completed,
            today = baseDate,
            targetDaysPerWeek = 3
        )
        assertEquals(3, streak)
    }

    @Test
    fun `weekly habit keeps streak alive mid-week if target is still reachable`() {
        // baseDate is Wednesday Oct 28, 2026.
        // Week 1 (current): Completed Oct 27 (1 so far), target = 3.
        // Days remaining in week: Oct 29, 30, 31, Nov 1 (4 days left). Total possible = 1 + 4 = 5 >= 3.
        // Prior week (Oct 19 - 25): 3 completions.
        val wednesday = LocalDate.of(2026, 10, 28)
        val completed = setOf(
            LocalDate.of(2026, 10, 27).toString(),
            LocalDate.of(2026, 10, 23).toString(),
            LocalDate.of(2026, 10, 21).toString(),
            LocalDate.of(2026, 10, 19).toString()
        )
        val streak = HabitStreakCalculator.calculateCurrentStreak(
            completedDates = completed,
            today = wednesday,
            targetDaysPerWeek = 3
        )
        // Prior week was 1, current week is still viable so prior streak of 1 is preserved
        assertEquals(1, streak)
    }

    @Test
    fun `weekly habit breaks streak if target is mathematically impossible`() {
        // baseDate is Sunday Nov 1, 2026 (last day of week).
        // Week 1 (current): Only completed Oct 27 (1 completion). Target = 3.
        // No more days left this week -> cannot reach target -> current streak is 0.
        val sunday = LocalDate.of(2026, 11, 1)
        val completed = setOf(
            LocalDate.of(2026, 10, 27).toString(),
            LocalDate.of(2026, 10, 23).toString(),
            LocalDate.of(2026, 10, 21).toString(),
            LocalDate.of(2026, 10, 19).toString()
        )
        val streak = HabitStreakCalculator.calculateCurrentStreak(
            completedDates = completed,
            today = sunday,
            targetDaysPerWeek = 3
        )
        assertEquals(0, streak)
    }
}
