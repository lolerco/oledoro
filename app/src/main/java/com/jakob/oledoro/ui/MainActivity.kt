package com.jakob.oledoro.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jakob.oledoro.OledoroApp
import com.jakob.oledoro.data.AppSettingsManager
import com.jakob.oledoro.data.GruvboxColor
import com.jakob.oledoro.domain.TimerEngine
import com.jakob.oledoro.domain.TimerEvent
import com.jakob.oledoro.domain.TimerState
import com.jakob.oledoro.domain.TimerStatus
import com.jakob.oledoro.service.TimerForegroundService
import com.jakob.oledoro.ui.ambient.AmbientController
import com.jakob.oledoro.ui.screens.AmbientScreen
import com.jakob.oledoro.ui.theme.OledPomodoroTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Shared singleton holder for TimerEngine ensuring MainActivity and TimerForegroundService
 * synchronize on the exact same timer state.
 */
object TimerEngineHolder {
    val engine: TimerEngine by lazy {
        val sm = OledoroApp.settingsManager
        TimerEngine(
            focusDurationMs = sm.focusMinutes.value.coerceIn(1, 90) * 60 * 1000L,
            shortBreakDurationMs = sm.shortBreakMinutes.value.coerceIn(1, 30) * 60 * 1000L,
            longBreakDurationMs = sm.longBreakMinutes.value.coerceIn(1, 60) * 60 * 1000L,
            totalRounds = sm.longBreakInterval.value.coerceIn(1, 10)
        )
    }
}

/**
 * ViewModel managing timer state, coroutines, and Gruvbox/dim settings.
 */
class MainViewModel : ViewModel() {
    private val engine: TimerEngine = TimerEngineHolder.engine
    private val settingsManager: AppSettingsManager = OledoroApp.settingsManager

    val timerState: StateFlow<TimerState> = engine.state
    val themeColor: StateFlow<GruvboxColor> = settingsManager.themeColor
    val breakColor: StateFlow<GruvboxColor> = settingsManager.breakColor
    val negativeColor: StateFlow<GruvboxColor> = settingsManager.negativeColor
    val isDimmingActive: StateFlow<Boolean> = settingsManager.isDimmingActive
    val dimPercentage: StateFlow<Int> = settingsManager.dimPercentage
    val focusMinutes: StateFlow<Int> = settingsManager.focusMinutes
    val shortBreakMinutes: StateFlow<Int> = settingsManager.shortBreakMinutes
    val longBreakMinutes: StateFlow<Int> = settingsManager.longBreakMinutes
    val longBreakInterval: StateFlow<Int> = settingsManager.longBreakInterval
    val autoBrightenOnFinish: StateFlow<Boolean> = settingsManager.autoBrightenOnFinish

    init {
        engine.events.onEach { event ->
            if (event is TimerEvent.PhaseCompleted) {
                // Two-way auto toggle: when timer is over, light up if auto toggle is on
                if (settingsManager.autoBrightenOnFinish.value) {
                    settingsManager.setDimmingActive(false)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun start() {
        // Two-way auto toggle: when starting any session from IDLE, dim the screen
        if (settingsManager.autoBrightenOnFinish.value && engine.state.value.status == TimerStatus.IDLE) {
            settingsManager.setDimmingActive(true)
        }
        engine.startTicker(viewModelScope)
    }

    fun pause() {
        engine.pause()
    }

    fun skip() {
        settingsManager.setDimmingActive(false)
        engine.nextPhase(autoStart = false)
    }

    fun reset() {
        engine.reset()
    }

    fun toggleDimming() {
        settingsManager.toggleDimming()
    }

    fun setThemeColor(color: GruvboxColor) {
        settingsManager.setThemeColor(color)
    }

    fun setBreakColor(color: GruvboxColor) {
        settingsManager.setBreakColor(color)
    }

    fun setNegativeColor(color: GruvboxColor) {
        settingsManager.setNegativeColor(color)
    }

    fun setDimPercentage(percentage: Int) {
        settingsManager.setDimPercentage(percentage)
    }

    fun setFocusMinutes(minutes: Int) {
        settingsManager.setFocusMinutes(minutes)
        syncEngineDurations()
    }

    fun setShortBreakMinutes(minutes: Int) {
        settingsManager.setShortBreakMinutes(minutes)
        syncEngineDurations()
    }

    fun setLongBreakMinutes(minutes: Int) {
        settingsManager.setLongBreakMinutes(minutes)
        syncEngineDurations()
    }

    fun setLongBreakInterval(interval: Int) {
        settingsManager.setLongBreakInterval(interval)
        syncEngineDurations()
    }

    fun setAutoBrightenOnFinish(enabled: Boolean) {
        settingsManager.setAutoBrightenOnFinish(enabled)
    }

    private fun syncEngineDurations() {
        engine.updateDurations(
            focusMinutes = settingsManager.focusMinutes.value,
            shortBreakMinutes = settingsManager.shortBreakMinutes.value,
            longBreakMinutes = settingsManager.longBreakMinutes.value,
            breakInterval = settingsManager.longBreakInterval.value
        )
    }
}

/**
 * Main entrance activity for oledoro:
 * - Configures screen-on, showWhenLocked, and window properties
 * - Integrates Compose AmbientScreen with Gruvbox themes, dimming, and TimerForegroundService
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var hasNotificationPermission by mutableStateOf(true)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted || NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

    private var hasRequestedPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure activity displays over lockscreen and can turn screen on
        AmbientController.configureShowWhenLocked(this)

        // Update notification permission state & request if needed
        updateNotificationPermissionState()
        requestNotificationPermissionIfNeeded()

        setContent {
            OledPomodoroTheme {
                val timerState by viewModel.timerState.collectAsStateWithLifecycle()
                val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
                val breakColor by viewModel.breakColor.collectAsStateWithLifecycle()
                val negativeColor by viewModel.negativeColor.collectAsStateWithLifecycle()
                val isDimmingActive by viewModel.isDimmingActive.collectAsStateWithLifecycle()
                val dimPercentage by viewModel.dimPercentage.collectAsStateWithLifecycle()
                val focusMinutes by viewModel.focusMinutes.collectAsStateWithLifecycle()
                val shortBreakMinutes by viewModel.shortBreakMinutes.collectAsStateWithLifecycle()
                val longBreakMinutes by viewModel.longBreakMinutes.collectAsStateWithLifecycle()
                val longBreakInterval by viewModel.longBreakInterval.collectAsStateWithLifecycle()
                val autoBrightenOnFinish by viewModel.autoBrightenOnFinish.collectAsStateWithLifecycle()

                AmbientScreen(
                    state = timerState,
                    themeColor = themeColor,
                    breakColor = breakColor,
                    negativeColor = negativeColor,
                    isDimmingActive = isDimmingActive,
                    dimPercentage = dimPercentage,
                    focusMinutes = focusMinutes,
                    shortBreakMinutes = shortBreakMinutes,
                    longBreakMinutes = longBreakMinutes,
                    longBreakInterval = longBreakInterval,
                    autoBrightenOnFinish = autoBrightenOnFinish,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestNotificationPermission = { openNotificationSettingsOrRequestPermission() },
                    onToggleDimming = { viewModel.toggleDimming() },
                    onThemeColorChange = { viewModel.setThemeColor(it) },
                    onBreakColorChange = { viewModel.setBreakColor(it) },
                    onNegativeColorChange = { viewModel.setNegativeColor(it) },
                    onDimPercentageChange = { viewModel.setDimPercentage(it) },
                    onFocusMinutesChange = { viewModel.setFocusMinutes(it) },
                    onShortBreakMinutesChange = { viewModel.setShortBreakMinutes(it) },
                    onLongBreakMinutesChange = { viewModel.setLongBreakMinutes(it) },
                    onLongBreakIntervalChange = { viewModel.setLongBreakInterval(it) },
                    onAutoBrightenOnFinishChange = { viewModel.setAutoBrightenOnFinish(it) },
                    onStart = {
                        viewModel.start()
                        TimerForegroundService.startService(this, TimerForegroundService.ACTION_START_SERVICE)
                    },
                    onPause = {
                        viewModel.pause()
                        TimerForegroundService.startService(this, TimerForegroundService.ACTION_SYNC)
                    },
                    onSkip = {
                        viewModel.skip()
                        TimerForegroundService.startService(this, TimerForegroundService.ACTION_SYNC)
                    },
                    onReset = {
                        viewModel.reset()
                        TimerForegroundService.startService(this, TimerForegroundService.ACTION_SYNC)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationPermissionState()
    }

    private fun updateNotificationPermissionState() {
        hasNotificationPermission = NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            hasRequestedPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openNotificationSettingsOrRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !hasRequestedPermission
        ) {
            hasRequestedPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openAppNotificationSettings()
        }
    }

    private fun openAppNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            } catch (_: Exception) {}
        }
    }
}
