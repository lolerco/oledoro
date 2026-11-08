package com.lolerco.oledoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.lolerco.oledoro.data.GruvboxColor
import com.lolerco.oledoro.habits.Habit
import com.lolerco.oledoro.habits.HabitStreakCalculator
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * General Habit Tracking Overview Sheet (revealed when swiping or clicking bottom summary bar).
 * Shows cross-habit aggregate statistics:
 * - Average weekly completion rate across all habits
 * - Average 30-day rate
 * - Today's completion rate
 * - Total active streaks sum
 * - Total check-ins recorded
 * - Per-habit breakdown summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabidoroOverviewBottomSheet(
    habits: List<Habit>,
    themeColor: GruvboxColor,
    today: LocalDate = LocalDate.now(),
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val statsList = remember(habits, today) {
        habits.map { habit ->
            habit to HabitStreakCalculator.calculateStats(habit, today)
        }
    }

    val totalDoneToday = remember(habits, today) {
        habits.count { it.completedDates.contains(today.toString()) }
    }

    val totalStreaksSum = remember(statsList) {
        statsList.sumOf { it.second.currentStreak }
    }

    val totalAllTimeChecks = remember(statsList) {
        statsList.sumOf { it.second.totalCompletions }
    }

    val avgWeeklyRate = remember(statsList) {
        if (statsList.isEmpty()) 0
        else (statsList.map { it.second.weeklyRatePercent }.average()).roundToInt().coerceIn(0, 100)
    }

    val avgMonthlyRate = remember(statsList) {
        if (statsList.isEmpty()) 0
        else (statsList.map { it.second.monthlyRatePercent }.average()).roundToInt().coerceIn(0, 100)
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
            // Header
            Text(
                text = "GENERAL OVERVIEW",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = themeColor.color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "GLOBAL HABIT TRACKING PERFORMANCE",
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                color = AmbientCoolGray.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Key Metrics Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewCard(
                    modifier = Modifier.weight(1f),
                    label = "AVG WEEKLY RATE",
                    value = "$avgWeeklyRate%",
                    accentColor = themeColor.color
                )
                OverviewCard(
                    modifier = Modifier.weight(1f),
                    label = "AVG 30-DAY RATE",
                    value = "$avgMonthlyRate%",
                    accentColor = themeColor.color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Key Metrics Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OverviewCard(
                    modifier = Modifier.weight(1f),
                    label = "TODAY",
                    value = "$totalDoneToday / ${habits.size}",
                    accentColor = Color.White
                )
                OverviewCard(
                    modifier = Modifier.weight(1f),
                    label = "STREAKS SUM",
                    value = "$totalStreaksSum",
                    accentColor = themeColor.color
                )
                OverviewCard(
                    modifier = Modifier.weight(1f),
                    label = "TOTAL CHECKS",
                    value = "$totalAllTimeChecks",
                    accentColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Breakdown list
            Text(
                text = "HABIT BREAKDOWN",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = AmbientCoolGray,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (habits.isEmpty()) {
                Text(
                    text = "No habits tracked yet",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = AmbientDimGray
                )
            } else {
                statsList.forEach { (habit, s) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F0F0F))
                            .border(1.dp, AmbientDimGray.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(habit.color.color)
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Column {
                                Text(
                                    text = habit.name,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (habit.targetDaysPerWeek == 7) "daily" else "${habit.targetDaysPerWeek}d/wk",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 10.sp,
                                    color = AmbientCoolGray.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${s.currentStreak} ${s.streakUnit}",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = habit.color.color
                                )
                                Text(
                                    text = "streak",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.sp,
                                    color = AmbientDimGray
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${s.weeklyRatePercent}%",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "7d rate",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 9.sp,
                                    color = AmbientDimGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
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
