package com.jakob.oledoro.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jakob.oledoro.data.AppSettingsManager
import com.jakob.oledoro.data.GruvboxColor
import com.jakob.oledoro.domain.*
import com.jakob.oledoro.ui.components.*
import com.jakob.oledoro.ui.theme.*
import com.jakob.oledoro.ui.utils.HapticHelper

@Composable
fun DesktopApp(engine: TimerEngine, settings: AppSettingsManager) {
    val timerState by engine.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var settingsDialogVisible by remember { mutableStateOf(false) }

    OledPomodoroTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack)
        ) {
            // Top Action Bar (Settings gear)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        HapticHelper.performClick(null)
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

            // Centered timer display
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isOvertime = timerState.isOvertime || timerState.status == TimerStatus.OVERTIME
                val isBreak = timerState.phase == TimerPhase.SHORT_BREAK || timerState.phase == TimerPhase.LONG_BREAK
                val clockColor = when {
                    isOvertime -> settings.negativeColor.color
                    isBreak -> settings.breakColor.color
                    else -> settings.themeColor.color
                }

                val phaseText = if (isOvertime) {
                    "OVERTIME • ${timerState.phase.displayName.uppercase()}"
                } else {
                    timerState.phase.displayName.uppercase()
                }

                Text(
                    text = phaseText,
                    color = AmbientCoolGray,
                    fontFamily = JetBrainsMono,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimerDisplay(
                    remainingMs = timerState.remainingMs,
                    isOvertime = isOvertime,
                    color = clockColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                NextPhasePreview(
                    previewText = timerState.nextPhasePreview,
                    color = AmbientCoolGray
                )
            }

            // Controls row - always visible, positioned below timer
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 150.dp)
            ) {
                ControlsRow(
                    status = timerState.status,
                    onStart = { engine.startTicker(coroutineScope) },
                    onPause = { engine.pause() },
                    onSkip = {
                        settings.setDimmingActive(false)
                        engine.nextPhase(autoStart = false)
                    },
                    onReset = { engine.reset() },
                    tint = AmbientCoolGray
                )
            }

            // Settings Dialog Modal
            if (settingsDialogVisible) {
                DesktopSettingsDialog(
                    selectedThemeColor = settings.themeColor,
                    onThemeColorChange = { settings.setThemeColor(it) },
                    selectedBreakColor = settings.breakColor,
                    onBreakColorChange = { settings.setBreakColor(it) },
                    selectedNegativeColor = settings.negativeColor,
                    onNegativeColorChange = { settings.setNegativeColor(it) },
                    focusMinutes = settings.focusMinutes,
                    onFocusMinutesChange = { settings.setFocusMinutes(it) },
                    shortBreakMinutes = settings.shortBreakMinutes,
                    onShortBreakMinutesChange = { settings.setShortBreakMinutes(it) },
                    longBreakMinutes = settings.longBreakMinutes,
                    onLongBreakMinutesChange = { settings.setLongBreakMinutes(it) },
                    longBreakInterval = settings.longBreakInterval,
                    onLongBreakIntervalChange = { settings.setLongBreakInterval(it) },
                    onDismiss = { settingsDialogVisible = false }
                )
            }
        }
    }
}

fun main() = application {
    val engine = TimerEngine()
    val settings = AppSettingsManager()
    Window(
        onCloseRequest = ::exitApplication,
        title = "oledoro",
        state = rememberWindowState(width = 600.dp, height = 800.dp)
    ) {
        DesktopApp(engine, settings)
    }
}