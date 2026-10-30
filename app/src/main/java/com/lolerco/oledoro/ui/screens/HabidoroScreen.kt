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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.data.GruvboxColor
import com.lolerco.oledoro.habits.Habit
import com.lolerco.oledoro.habits.HabitStreakCalculator
import com.lolerco.oledoro.ui.ambient.AmbientController
import com.lolerco.oledoro.ui.ambient.AmbientController.burnInShift
import com.lolerco.oledoro.ui.components.AddHabitDialog
import com.lolerco.oledoro.ui.components.HabitRow
import com.lolerco.oledoro.ui.components.HabitStatsBottomSheet
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import com.lolerco.oledoro.ui.utils.HapticHelper
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Habidoro mini app screen: OLED-optimized habit tracker mirroring uhabits (Loop Habit Tracker).
 * - Top title "habidoro"
 * - Streak recognition: streaks on left, habit name center, past week 7-day checks right
 * - Swipe up from bottom reveals full statistics sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabidoroScreen(
    habits: List<Habit>,
    themeColor: GruvboxColor,
    isDimmingActive: Boolean,
    dimPercentage: Int,
    onToggleDimming: () -> Unit,
    onToggleHabitCompletion: (habitId: String, dateString: String) -> Unit,
    onAddHabit: (name: String, color: GruvboxColor) -> Unit,
    onDeleteHabit: (habitId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedHabitForStats by remember { mutableStateOf<Habit?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // When the stats bottom sheet is open, temporarily illuminate screen
    val effectiveDimming = if (sheetState.isVisible) false else isDimmingActive

    AmbientController.AmbientModeEffect(
        isDimmed = effectiveDimming,
        dimPercentage = dimPercentage
    )

    val burnInOffset by AmbientController.rememberBurnInShift()

    // Global overview stats across all habits for bottom summary card
    val totalActiveStreak = remember(habits) {
        habits.sumOf { HabitStreakCalculator.calculateCurrentStreak(it.completedDates, today) }
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
                    // Dimming toggle
                    IconButton(
                        onClick = {
                            HapticHelper.performClick(context)
                            onToggleDimming()
                        }
                    ) {
                        if (!isDimmingActive) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = "Bright Screen",
                                tint = themeColor.color,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = "Dim Screen",
                                tint = AmbientDimGray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Top Title: "habidoro" matching oledoro styling
                    Text(
                        text = "habidoro",
                        color = AmbientCoolGray,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

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
                                    scope.launch { sheetState.show() }
                                }
                            )
                        }
                    }
                }
            }

        // Bottom summary & swipe-up trigger
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.performClick(context)
                    // Open stats for the first habit or currently focused habit
                    if (selectedHabitForStats == null && habits.isNotEmpty()) {
                        selectedHabitForStats = habits.first()
                    }
                    scope.launch { sheetState.show() }
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
                    text = "today: $totalDoneToday/${habits.size} done  |  total streaks: ${totalActiveStreak}d  ▲",
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = AmbientCoolGray.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Habit Stats Bottom Sheet
        if (sheetState.isVisible && selectedHabitForStats != null) {
            val currentHabit = habits.find { it.id == selectedHabitForStats?.id } ?: selectedHabitForStats!!
            HabitStatsBottomSheet(
                sheetState = sheetState,
                habit = currentHabit,
                today = today,
                onDeleteHabit = { habitId ->
                    onDeleteHabit(habitId)
                    scope.launch { sheetState.hide() }
                },
                onDismissRequest = {
                    scope.launch { sheetState.hide() }
                }
            )
        }

        // Add Habit Dialog
        if (showAddDialog) {
            AddHabitDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, color ->
                    onAddHabit(name, color)
                    showAddDialog = false
                }
            )
        }
    }
}
