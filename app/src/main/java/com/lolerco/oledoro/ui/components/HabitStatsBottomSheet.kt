package com.lolerco.oledoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
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
 * Slide-up bottom sheet displaying deep habit statistics matching uhabits (Loop Habit Tracker):
 * - Current streak & Best streak
 * - Total repetitions
 * - Weekly rate (past 7 days %)
 * - Monthly rate (past 30 days %)
 * - All-time rate (%)
 * - 30-day activity matrix
 * - Delete habit option
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitStatsBottomSheet(
    sheetState: SheetState,
    habit: Habit,
    today: LocalDate = LocalDate.now(),
    onDeleteHabit: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val stats = remember(habit.completedDates, today) {
        HabitStreakCalculator.calculateStats(habit, today)
    }
    val past30Days = remember(habit.completedDates, today) {
        HabitStreakCalculator.getPast30Days(habit, today)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Black,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AmbientDimGray.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header row with habit title & delete button
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
                        fontSize = 16.sp,
                        color = habit.color.composeColor
                    )
                    Text(
                        text = "HABIT STATISTICS & STREAKS",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = AmbientCoolGray.copy(alpha = 0.8f)
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

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Stats Cards (uhabits streak cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "CURRENT STREAK",
                    value = "${stats.currentStreak} d",
                    accentColor = habit.color.composeColor
                )
                HabitMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "BEST STREAK",
                    value = "${stats.bestStreak} d",
                    accentColor = habit.color.composeColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Stats Cards (Rates & Totals)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    accentColor = habit.color.composeColor
                )
                HabitMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "30-DAY RATE",
                    value = "${stats.monthlyRatePercent}%",
                    accentColor = habit.color.composeColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 30-Day Activity History Grid
            Text(
                text = "30-DAY HISTORY",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = AmbientCoolGray,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                past30Days.forEach { day ->
                    HistoryDayDot(
                        day = day,
                        accentColor = habit.color.composeColor
                    )
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
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = AmbientCoolGray.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = accentColor
            )
        }
    }
}

@Composable
private fun HistoryDayDot(
    day: DayCheck,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (day.isCompleted) accentColor else Color(0xFF161616)
            )
            .border(
                width = if (day.isToday) 1.dp else 0.5.dp,
                color = if (day.isToday) Color.White else AmbientDimGray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${day.dayNumber}",
            fontFamily = JetBrainsMono,
            fontSize = 9.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isCompleted) Color.Black else AmbientCoolGray.copy(alpha = 0.6f)
        )
    }
}
