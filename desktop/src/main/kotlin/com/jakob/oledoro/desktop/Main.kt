package com.jakob.oledoro.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.nio.file.Paths

@Composable
fun DesktopApp(engine: TimerEngine, settings: AppSettingsManager) {
    val timerState by engine.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    OledPomodoroTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack)
        ) {
            // Centered timer display (mirrors AmbientScreen, no ambient/dimming)
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

            // Controls row at bottom center
            var controlsVisible by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { controlsVisible = !controlsVisible },
                contentAlignment = Alignment.Center
            ) {
                if (controlsVisible) {
                    Box(modifier = Modifier.align(Alignment.Center).offset(y = 120.dp)) {
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
                }
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
