package com.lolerco.oledoro.habits

import com.lolerco.oledoro.data.GruvboxColor
import java.time.LocalDate

/**
 * Data model representing a habit tracked in Habidoro.
 */
data class Habit(
    val id: String,
    val name: String,
    val color: GruvboxColor = GruvboxColor.YELLOW,
    val createdAt: String = LocalDate.now().toString(),
    val completedDates: Set<String> = emptySet(),
    val targetDaysPerWeek: Int = 7
)

/**
 * Detailed statistics for a habit mirroring uhabits (Loop Habit Tracker) analytics.
 */
data class HabitStats(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val weeklyRatePercent: Int,
    val monthlyRatePercent: Int,
    val allTimeRatePercent: Int
)

/**
 * A single day in the weekly streak view (past 7 days).
 */
data class DayCheck(
    val date: LocalDate,
    val dateString: String,
    val dayInitial: String,
    val dayNumber: Int,
    val isCompleted: Boolean,
    val isToday: Boolean
)
