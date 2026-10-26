package com.lolerco.oledoro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.R
import com.lolerco.oledoro.data.GruvboxColor
import com.lolerco.oledoro.domain.TimerPhase
import com.lolerco.oledoro.domain.TimerState
import com.lolerco.oledoro.domain.TimerStatus
import com.lolerco.oledoro.ui.ambient.AmbientModeEffect
import com.lolerco.oledoro.ui.ambient.burnInShift
import com.lolerco.oledoro.ui.ambient.rememberBurnInShift
import com.lolerco.oledoro.ui.components.ControlsRow
import com.lolerco.oledoro.ui.components.NextPhasePreview
import com.lolerco.oledoro.ui.components.SettingsDialog
import com.lolerco.oledoro.ui.components.TimerDisplay
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import com.lolerco.oledoro.ui.theme.OledBlack
import com.lolerco.oledoro.ui.theme.OvertimeRed
import com.lolerco.oledoro.ui.utils.HapticHelper

@Composable
fun AmbientScreen(
    state: TimerState,
    themeColor: GruvboxColor,
    breakColor: GruvboxColor = GruvboxColor.AQUA,
    negativeColor: GruvboxColor = GruvboxColor.RED,
    isDimmingActive: Boolean,
    dimPercentage: Int,
    focusMinutes: Int = 25,
    shortBreakMinutes: Int = 5,
    longBreakMinutes: Int = 15,
    longBreakInterval: Int = 4,
    autoBrightenOnFinish: Boolean = true,
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onToggleDimming: () -> Unit,
    onThemeColorChange: (GruvboxColor) -> Unit,
    onBreakColorChange: (GruvboxColor) -> Unit = {},
    onNegativeColorChange: (GruvboxColor) -> Unit = {},
    onDimPercentageChange: (Int) -> Unit,
    onFocusMinutesChange: (Int) -> Unit = {},
    onShortBreakMinutesChange: (Int) -> Unit = {},
    onLongBreakMinutesChange: (Int) -> Unit = {},
    onLongBreakIntervalChange: (Int) -> Unit = {},
    onAutoBrightenOnFinishChange: (Boolean) -> Unit = {},
    onStart: () -> Unit,
    onPause: () -> Unit,
    onFastForward: () -> Unit = {},
    onSkip: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    enableBurnInShift: Boolean = true
) {
    // Window management: dynamic screenBrightness based on lightbulb toggle and slider
    AmbientModeEffect(
        dimmingEnabled = isDimmingActive,
        dimPercentage = dimPercentage
    )

    // Anti-burn-in displacement offset
    val burnInOffset by rememberBurnInShift(enabled = enableBurnInShift)

    // Controls, Settings and Study Stats visibility states
    var controlsVisible by remember { mutableStateOf(false) }
    var settingsDialogVisible by remember { mutableStateOf(false) }
    var statsDialogVisible by remember { mutableStateOf(false) }
    val statsRepo = remember { com.lolerco.oledoro.data.AndroidStudyStatsRepository(context) }

    val isOvertime = state.isOvertime || state.status == TimerStatus.OVERTIME
    val isBreak = state.phase == TimerPhase.SHORT_BREAK || state.phase == TimerPhase.LONG_BREAK
    val clockColor = when {
        isOvertime -> negativeColor.color
        isBreak -> breakColor.color
        else -> themeColor.color
    }

    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        // Top Action Bar (Lightbulb toggle & Gear Settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lightbulb button: toggles total dimming (reversed logic: bright = illuminated, dim = unlit)
            IconButton(
                onClick = {
                    HapticHelper.performClick(context)
                    onToggleDimming()
                }
            ) {
                if (!isDimmingActive) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = "Bright Screen (Illuminated)",
                        tint = themeColor.color,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = "Dim Screen (Unlit)",
                        tint = AmbientDimGray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Gear button: opens SettingsDialog
            IconButton(
                onClick = {
                    HapticHelper.performClick(context)
                    settingsDialogVisible = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = AmbientCoolGray,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // 1. Center Timer Display: strictly centered in the Box!
        // Never shifts or jumps when action controls appear/disappear.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .burnInShift(burnInOffset)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Phase label (e.g. FOCUS, SHORT BREAK, or OVERTIME)
            val phaseText = if (isOvertime) {
                "OVERTIME • ${state.phase.displayName.uppercase()}"
            } else {
                state.phase.displayName.uppercase()
            }

            Text(
                text = phaseText,
                color = AmbientCoolGray,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Large JetBrains Mono countdown / overtime digits
            TimerDisplay(
                remainingMs = state.remainingMs,
                isOvertime = isOvertime,
                color = clockColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Next Phase Preview (e.g. "NEXT: 5 MIN BREAK")
            NextPhasePreview(
                previewText = state.nextPhasePreview,
                color = AmbientCoolGray
            )
        }

        // 2. Action Controls: Positioned below the timer block
        // Decoupled from the Timer Column so the timer NEVER shifts.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .burnInShift(burnInOffset)
                .offset(y = 150.dp)
        ) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ControlsRow(
                    status = state.status,
                    onStart = onStart,
                    onPause = onPause,
                    onFastForward = onFastForward,
                    onSkip = onSkip,
                    onReset = onReset,
                    tint = AmbientCoolGray
                )
            }
        }

        // Notification permission banner (unobtrusive hint at bottom of screen)
        if (!hasNotificationPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .burnInShift(burnInOffset)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .background(
                        color = Color(0x33FABD2F),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x66FABD2F),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        HapticHelper.performClick(context)
                        onRequestNotificationPermission()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.notification_permission_banner),
                    color = Color(0xFFFABD2F),
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Total time counters at bottom inside an ascii-like box
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .burnInShift(burnInOffset)
                .padding(bottom = 30.dp)
                .clickable {
                    HapticHelper.performClick(context)
                    statsDialogVisible = true
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .border(1.dp, AmbientDimGray, RoundedCornerShape(4.dp))
                    .background(Color(0xFF0C0C0C), RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "[ your stats ]",
                    color = themeColor.color,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "total focus time: ${formatTime(state.totalFocusTimeMs)}",
                    color = AmbientCoolGray,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "total break time: ${formatTime(state.totalBreakTimeMs)}",
                    color = AmbientCoolGray,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Settings Dialog Modal
        if (settingsDialogVisible) {
            SettingsDialog(
                selectedThemeColor = themeColor,
                onThemeColorChange = onThemeColorChange,
                selectedBreakColor = breakColor,
                onBreakColorChange = onBreakColorChange,
                selectedNegativeColor = negativeColor,
                onNegativeColorChange = onNegativeColorChange,
                dimPercentage = dimPercentage,
                onDimPercentageChange = onDimPercentageChange,
                focusMinutes = focusMinutes,
                onFocusMinutesChange = onFocusMinutesChange,
                shortBreakMinutes = shortBreakMinutes,
                onShortBreakMinutesChange = onShortBreakMinutesChange,
                longBreakMinutes = longBreakMinutes,
                onLongBreakMinutesChange = onLongBreakMinutesChange,
                longBreakInterval = longBreakInterval,
                onLongBreakIntervalChange = onLongBreakIntervalChange,
                autoBrightenOnFinish = autoBrightenOnFinish,
                onAutoBrightenOnFinishChange = onAutoBrightenOnFinishChange,
                onDismiss = { settingsDialogVisible = false }
            )
        }

        // Study Stats Dialog Modal
        if (statsDialogVisible) {
            com.lolerco.oledoro.ui.components.StudyStatsDialog(
                dailyStats = statsRepo.getRecentDays(7),
                focusColor = themeColor.color,
                breakColor = breakColor.color,
                onDismiss = { statsDialogVisible = false }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
