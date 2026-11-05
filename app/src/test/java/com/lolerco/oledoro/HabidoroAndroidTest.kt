package com.lolerco.oledoro

import com.lolerco.oledoro.data.GruvboxColor
import com.lolerco.oledoro.habits.Habit
import com.lolerco.oledoro.habits.HabitStreakCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HabidoroAndroidTest {

    @Test
    fun test3StateCycleLogic() {
        val today = "2026-05-11"
        var habit = Habit(
            id = "h1",
            name = "Meditate",
            color = GruvboxColor.BLUE
        )

        // 1. OFF -> ON
        val completed1 = habit.completedDates.toMutableSet()
        val skipped1 = habit.skippedDates.toMutableSet()
        completed1.add(today)
        habit = habit.copy(completedDates = completed1, skippedDates = skipped1)
        assertTrue(habit.completedDates.contains(today))
        assertFalse(habit.skippedDates.contains(today))

        // 2. ON -> SKIPPED
        val completed2 = habit.completedDates.toMutableSet()
        val skipped2 = habit.skippedDates.toMutableSet()
        completed2.remove(today)
        skipped2.add(today)
        habit = habit.copy(completedDates = completed2, skippedDates = skipped2)
        assertFalse(habit.completedDates.contains(today))
        assertTrue(habit.skippedDates.contains(today))

        // 3. SKIPPED -> OFF
        val completed3 = habit.completedDates.toMutableSet()
        val skipped3 = habit.skippedDates.toMutableSet()
        skipped3.remove(today)
        habit = habit.copy(completedDates = completed3, skippedDates = skipped3)
        assertFalse(habit.completedDates.contains(today))
        assertFalse(habit.skippedDates.contains(today))
    }

    @Test
    fun testStreakCalculatorWithSkippedDays() {
        val today = LocalDate.of(2026, 5, 11)
        val completed = setOf("2026-05-09", "2026-05-11")
        val skipped = setOf("2026-05-10")
        val streak = HabitStreakCalculator.calculateCurrentStreak(completed, skipped, today)
        assertEquals(2, streak)
    }

    @Test
    fun testPast4DaysGeneration() {
        val today = LocalDate.of(2026, 5, 11)
        val habit = Habit(id = "h1", name = "Read", color = GruvboxColor.AQUA)
        val past4 = HabitStreakCalculator.getPast4Days(habit, today)
        assertEquals(4, past4.size)
        assertEquals("2026-05-11", past4.last().dateString)
        assertTrue(past4.last().isToday)
    }
}
