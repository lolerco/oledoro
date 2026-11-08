package com.lolerco.oledoro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.data.GruvboxColor
import com.lolerco.oledoro.habits.Habit
import com.lolerco.oledoro.habits.HabitStreakCalculator
import com.lolerco.oledoro.ui.components.AddHabitDialog
import com.lolerco.oledoro.ui.components.EditHabitDialog
import com.lolerco.oledoro.ui.components.HabidoroOverviewBottomSheet
import com.lolerco.oledoro.ui.components.HabitRow
import com.lolerco.oledoro.ui.components.HabitStatsBottomSheet
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import com.lolerco.oledoro.ui.ambient.burnInShift
import com.lolerco.oledoro.ui.ambient.rememberBurnInShift
import com.lolerco.oledoro.ui.utils.HapticHelper
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Habidoro mini app screen: OLED-optimized habit tracker mirroring uhabits (Loop Habit Tracker).
 * - Top title "habidoro" with quick Add Habit button and App Settings button
 * - Streak recognition: streaks on left, habit name center, past 4 days right
 * - Tapping any habit opens its dedicated stats & settings sheet with 365-day editable history
 * - Swiping or tapping the bottom bar reveals general aggregate habit tracking stats (average weekly rate, etc.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabidoroScreen(
    habits: List<Habit>,
    themeColor: GruvboxColor,
    onOpenSettings: () -> Unit = {},
    onToggleHabitCompletion: (habitId: String, dateString: String) -> Unit,
    onAddHabit: (name: String, color: GruvboxColor, targetDaysPerWeek: Int) -> Unit,
    onUpdateHabit: (habitId: String, name: String, color: GruvboxColor, targetDaysPerWeek: Int) -> Unit = { _, _, _, _ -> },
    onDeleteHabit: (habitId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

    var selectedHabitForStats by remember { mutableStateOf<Habit?>(null) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var overviewSheetVisible by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val burnInOffset by rememberBurnInShift()

    // Global overview stats across all habits for bottom summary card
    val totalActiveStreak = remember(habits) {
        habits.sumOf { HabitStreakCalculator.calculateCurrentStreak(it, today) }
    }
    val totalDoneToday = remember(habits) {
        habits.count { it.completedDates.contains(today.toString()) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .burnInShift(burnInOffset)
        ) {
            // Top Action Bar with "habidoro" title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add habit button in action bar
                IconButton(
                    onClick = {
                        HapticHelper.performClick(context)
                        showAddDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Habit",
                        tint = themeColor.color,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Top Title: "habidoro" matching oledoro styling
                Text(
                    text = "habidoro",
                    color = AmbientCoolGray,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                // Settings button
                IconButton(
                    onClick = {
                        HapticHelper.performClick(context)
                        onOpenSettings()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = themeColor.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Habits list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 60.dp)
            ) {
                if (habits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "no habits yet\ntap + to track a habit",
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                                color = AmbientDimGray
                            )
                        }
                    }
                } else {
                    items(habits, key = { it.id }) { habit ->
                        HabitRow(
                            habit = habit,
                            today = today,
                            onToggleDay = { dateStr ->
                                HapticHelper.performClick(context)
                                onToggleHabitCompletion(habit.id, dateStr)
                            },
                            onSelectHabitForStats = {
                                HapticHelper.performClick(context)
                                selectedHabitForStats = habit
                            }
                        )
                    }
                }
            }
        }

        // Bottom summary & general tracking stats swipe-up trigger
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.performClick(context)
                    overviewSheetVisible = true
                }
                .padding(bottom = 20.dp, top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(AmbientDimGray.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "today: $totalDoneToday/${habits.size} done  |  streaks: ${totalActiveStreak}  ▲",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = AmbientCoolGray.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // 1. Habit-Specific Stats & Settings Bottom Sheet (shown only when clicking on a habit)
        if (selectedHabitForStats != null) {
            val currentHabit = habits.find { it.id == selectedHabitForStats?.id } ?: selectedHabitForStats!!
            HabitStatsBottomSheet(
                habit = currentHabit,
                today = today,
                onEditHabit = {
                    habitToEdit = currentHabit
                },
                onDeleteHabit = { habitId ->
                    onDeleteHabit(habitId)
                    selectedHabitForStats = null
                },
                onToggleDate = { dateStr ->
                    HapticHelper.performClick(context)
                    onToggleHabitCompletion(currentHabit.id, dateStr)
                },
                onDismissRequest = {
                    selectedHabitForStats = null
                }
            )
        }

        // 2. General Habit Tracking Overview Sheet (revealed when swiping/tapping bottom summary)
        if (overviewSheetVisible) {
            HabidoroOverviewBottomSheet(
                habits = habits,
                themeColor = themeColor,
                today = today,
                onDismissRequest = {
                    overviewSheetVisible = false
                }
            )
        }

        // 3. Edit Habit Dialog
        if (habitToEdit != null) {
            val habit = habitToEdit!!
            EditHabitDialog(
                habit = habit,
                onDismiss = { habitToEdit = null },
                onConfirm = { name, color, targetDays ->
                    onUpdateHabit(habit.id, name, color, targetDays)
                    habitToEdit = null
                }
            )
        }

        // 4. Add Habit Dialog
        if (showAddDialog) {
            AddHabitDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, color, targetDays ->
                    onAddHabit(name, color, targetDays)
                    showAddDialog = false
                }
            )
        }
    }
}
