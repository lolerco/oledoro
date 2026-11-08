package com.lolerco.oledoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.habits.DayCheck
import com.lolerco.oledoro.habits.Habit
import com.lolerco.oledoro.habits.HabitStreakCalculator
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import java.time.LocalDate

/**
 * Detailed Stats & Settings Bottom Sheet for a single Habit.
 * Shown exclusively when tapping the habit row.
 * Includes:
 * - Habit title and target frequency
 * - Edit and Delete actions
 * - Current & Best Streaks
 * - Total checks, 7-day rate, 30-day rate, all-time rate
 * - 365-day uhabits-style interactive history grid (7 rows x 52 columns) where tapping past days toggles their state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitStatsBottomSheet(
    habit: Habit,
    today: LocalDate = LocalDate.now(),
    onEditHabit: () -> Unit,
    onDeleteHabit: (habitId: String) -> Unit,
    onToggleDate: (dateString: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val stats = remember(habit.completedDates, habit.skippedDates, today) {
        HabitStreakCalculator.calculateStats(habit, today)
    }

    // 364 days back to today = 365 days total (52 weeks of 7 days plus remainder)
    val pastYearDays = remember(habit.completedDates, habit.skippedDates, today) {
        HabitStreakCalculator.getPastDays(habit, daysCount = 365, today = today)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Black,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AmbientDimGray)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header row with Name, Edit, and Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name.uppercase(),
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = habit.color.color
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "TARGET: ${if (habit.targetDaysPerWeek == 7) "EVERY DAY (7d/wk)" else "${habit.targetDaysPerWeek} DAYS / WEEK"}",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = AmbientCoolGray.copy(alpha = 0.8f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditHabit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Habit Settings",
                            tint = habit.color.color
                        )
                    }
                    IconButton(onClick = { onDeleteHabit(habit.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Habit",
                            tint = AmbientDimGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Stats Cards (uhabits streak cards)
            val unit = if (habit.targetDaysPerWeek == 7) "d" else if (stats.currentStreak == 1) "wk" else "wks"
            val bestUnit = if (habit.targetDaysPerWeek == 7) "d" else if (stats.bestStreak == 1) "wk" else "wks"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "CURRENT STREAK",
                    value = "${stats.currentStreak} $unit",
                    accentColor = habit.color.color
                )
                HabitMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "BEST STREAK",
                    value = "${stats.bestStreak} $bestUnit",
                    accentColor = habit.color.color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Stats Cards (Rates & Totals)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "TOTAL CHECKS",
                    value = "${stats.totalCompletions}",
                    accentColor = Color.White
                )
                HabitMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "7-DAY RATE",
                    value = "${stats.weeklyRatePercent}%",
                    accentColor = habit.color.color
                )
                HabitMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "30-DAY RATE",
                    value = "${stats.monthlyRatePercent}%",
                    accentColor = habit.color.color
                )
                HabitMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "ALL-TIME",
                    value = "${stats.allTimeRatePercent}%",
                    accentColor = habit.color.color
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 365-Day uhabits Interactive History Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "365-DAY HISTORY (TAP TO EDIT)",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = AmbientCoolGray,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "← scroll →",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = AmbientDimGray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any past day to toggle: off → completed → skipped",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = AmbientDimGray
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Calendar contribution-style grid: 7 rows (Mon-Sun), columns for weeks
            val horizontalScrollState = rememberScrollState(initial = Int.MAX_VALUE)
            val weekColumns = remember(pastYearDays) {
                pastYearDays.chunked(7)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekColumns.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { day ->
                            HistoryDayTile(
                                day = day,
                                accentColor = habit.color.color,
                                onClick = { onToggleDate(day.dateString) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, AmbientDimGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .background(Color(0xFF0F0F0F))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontSize = 9.sp,
                color = AmbientCoolGray.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = accentColor
            )
        }
    }
}

@Composable
private fun HistoryDayTile(
    day: DayCheck,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(19.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(
                when {
                    day.isCompleted -> accentColor
                    day.isSkipped -> accentColor.copy(alpha = 0.35f)
                    else -> Color(0xFF141414)
                }
            )
            .border(
                width = if (day.isToday) 1.dp else 0.5.dp,
                color = when {
                    day.isCompleted -> accentColor
                    day.isSkipped -> accentColor
                    day.isToday -> Color.White
                    else -> AmbientDimGray.copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(3.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (day.isToday) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (day.isCompleted) Color.Black else Color.White)
            )
        }
    }
}
