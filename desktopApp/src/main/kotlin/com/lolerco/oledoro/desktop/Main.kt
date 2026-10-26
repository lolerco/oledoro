package com.lolerco.oledoro.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.lolerco.oledoro.data.AppSettingsManager
import com.lolerco.oledoro.data.DesktopStudyStatsRepository
import com.lolerco.oledoro.data.GruvboxColor
import com.lolerco.oledoro.domain.*
import com.lolerco.oledoro.ui.components.*
import com.lolerco.oledoro.ui.theme.*
import com.lolerco.oledoro.ui.utils.HapticHelper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun DesktopApp(engine: TimerEngine, settings: AppSettingsManager, statsRepo: DesktopStudyStatsRepository = remember { DesktopStudyStatsRepository() }) {
    val timerState by engine.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var settingsDialogVisible by remember { mutableStateOf(false) }
    var statsDialogVisible by remember { mutableStateOf(false) }

    // Sync engine time accumulation to stats repo
    LaunchedEffect(engine, statsRepo) {
        engine.onTimeAccumulated = { focusDelta, breakDelta ->
            statsRepo.recordTime(focusDelta, breakDelta)
        }
    }

    // Observe timer events for notifications
    LaunchedEffect(engine) {
        engine.events.onEach { event ->
            when (event) {
                is TimerEvent.PhaseCompleted -> {
                    statsRepo.flush()
                    val phaseName = event.phase.displayName
                    val nextPhaseName = when (event.phase) {
                        TimerPhase.FOCUS -> "Break"
                        TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> "Focus"
                    }
                    DesktopNotificationManager.notifyTimerFinished(phaseName, nextPhaseName)
                }
            }
        }.launchIn(coroutineScope)
    }

    // Initialize notifications on startup
    LaunchedEffect(Unit) {
        DesktopNotificationManager.initialize(coroutineScope)
    }

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
                    onFastForward = {
                        if (settings.autoBrightenOnFinish) {
                            settings.updateDimmingActive(true)
                        }
                        engine.nextPhase(autoStart = true)
                        engine.startTicker(coroutineScope)
                    },
                    onSkip = {
                        settings.updateDimmingActive(false)
                        engine.nextPhase(autoStart = false)
                    },
                    onReset = { engine.reset() },
                    tint = AmbientCoolGray
                )
            }

            // Total time counters at bottom inside an ascii-like box
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
                    .clickable {
                        HapticHelper.performClick(null)
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
                        color = settings.themeColor.color,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "total focus time: ${formatTime(engine.totalFocusTimeMs)} · total break time: ${formatTime(engine.totalBreakTimeMs)}",
                        color = AmbientCoolGray,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Settings Dialog Modal
            if (settingsDialogVisible) {
                DesktopSettingsDialog(
                    selectedThemeColor = settings.themeColor,
                    onThemeColorChange = { settings.updateThemeColor(it) },
                    selectedBreakColor = settings.breakColor,
                    onBreakColorChange = { settings.updateBreakColor(it) },
                    selectedNegativeColor = settings.negativeColor,
                    onNegativeColorChange = { settings.updateNegativeColor(it) },
                    focusMinutes = settings.focusMinutes,
                    onFocusMinutesChange = {
                        settings.updateFocusMinutes(it)
                        engine.updateDurations(
                            focusMinutes = settings.focusMinutes,
                            shortBreakMinutes = settings.shortBreakMinutes,
                            longBreakMinutes = settings.longBreakMinutes,
                            breakInterval = settings.longBreakInterval
                        )
                    },
                    shortBreakMinutes = settings.shortBreakMinutes,
                    onShortBreakMinutesChange = {
                        settings.updateShortBreakMinutes(it)
                        engine.updateDurations(
                            focusMinutes = settings.focusMinutes,
                            shortBreakMinutes = settings.shortBreakMinutes,
                            longBreakMinutes = settings.longBreakMinutes,
                            breakInterval = settings.longBreakInterval
                        )
                    },
                    longBreakMinutes = settings.longBreakMinutes,
                    onLongBreakMinutesChange = {
                        settings.updateLongBreakMinutes(it)
                        engine.updateDurations(
                            focusMinutes = settings.focusMinutes,
                            shortBreakMinutes = settings.shortBreakMinutes,
                            longBreakMinutes = settings.longBreakMinutes,
                            breakInterval = settings.longBreakInterval
                        )
                    },
                    longBreakInterval = settings.longBreakInterval,
                    onLongBreakIntervalChange = {
                        settings.updateLongBreakInterval(it)
                        engine.updateDurations(
                            focusMinutes = settings.focusMinutes,
                            shortBreakMinutes = settings.shortBreakMinutes,
                            longBreakMinutes = settings.longBreakMinutes,
                            breakInterval = settings.longBreakInterval
                        )
                    },
                    onDismiss = { settingsDialogVisible = false }
                )
            }

            // Study Stats Dialog Modal
            if (statsDialogVisible) {
                // Read stats from statsRepo (trigger ensures update when time recorded)
                @Suppress("UNUSED_VARIABLE")
                val trigger = statsRepo.updateTrigger
                com.lolerco.oledoro.ui.components.StudyStatsDialog(
                    dailyStats = statsRepo.getAllTrackedDays(),
                    focusColor = settings.themeColor.color,
                    breakColor = settings.breakColor.color,
                    accentColor = settings.themeColor.color,
                    onDismiss = { statsDialogVisible = false }
                )
            }
        }
    }
}

fun main() = application {
    val settings = AppSettingsManager()
    val engine = TimerEngine(
        focusDurationMs = settings.focusMinutes * 60 * 1000L,
        shortBreakDurationMs = settings.shortBreakMinutes * 60 * 1000L,
        longBreakDurationMs = settings.longBreakMinutes * 60 * 1000L,
        totalRounds = settings.longBreakInterval
    )
    Window(
        onCloseRequest = {
            DesktopNotificationManager.cleanup()
            exitApplication()
        },
        title = "oledoro",
        state = rememberWindowState(width = 600.dp, height = 800.dp)
    ) {
        DesktopApp(engine, settings)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}