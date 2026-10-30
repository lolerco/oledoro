package com.lolerco.oledoro.habits

import com.lolerco.oledoro.data.GruvboxColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabidoroDomainTest {

    private val today = LocalDate.of(2026, 10, 30)

    @Test
    fun `uhabits current streak accounts for missed or active today`() {
        // Case 1: completed today and yesterday -> streak = 2
        val habit1 = Habit(
            id = "1",
            name = "Study",
            completedDates = setOf(today.toString(), today.minusDays(1).toString())
        )
        assertEquals(2, HabitStreakCalculator.calculateCurrentStreak(habit1.completedDates, today))

        // Case 2: completed yesterday but not yet today -> streak = 1 (alive in uhabits)
        val habit2 = Habit(
            id = "2",
            name = "Meditate",
            completedDates = setOf(today.minusDays(1).toString())
        )
        assertEquals(1, HabitStreakCalculator.calculateCurrentStreak(habit2.completedDates, today))

        // Case 3: missed yesterday -> streak = 0
        val habit3 = Habit(
            id = "3",
            name = "Walk",
            completedDates = setOf(today.minusDays(2).toString())
        )
        assertEquals(0, HabitStreakCalculator.calculateCurrentStreak(habit3.completedDates, today))
    }

    @Test
    fun `past 30 days history produces 30 days ending today`() {
        val habit = Habit(id = "1", name = "Habit")
        val history = HabitStreakCalculator.getPast30Days(habit, today)
        assertEquals(30, history.size)
        assertEquals(today.minusDays(29), history.first().date)
        assertEquals(today, history.last().date)
        assertTrue(history.last().isToday)
    }

    @Test
    fun `rates calculation matches expected percentages`() {
        // 7 completions in 7 days -> 100% weekly rate
        val dates = (0..6).map { today.minusDays(it.toLong()).toString() }.toSet()
        val habit = Habit(
            id = "1",
            name = "Daily Habit",
            createdAt = today.minusDays(6).toString(),
            completedDates = dates
        )
        val stats = HabitStreakCalculator.calculateStats(habit, today)
        assertEquals(7, stats.currentStreak)
        assertEquals(7, stats.bestStreak)
        assertEquals(7, stats.totalCompletions)
        assertEquals(100, stats.weeklyRatePercent)
        assertEquals(100, stats.allTimeRatePercent)
    }
}
