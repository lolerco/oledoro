package com.jakob.oledoro.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.jakob.oledoro.OledoroApp
import com.jakob.oledoro.domain.TimerEngine
import com.jakob.oledoro.domain.TimerEvent
import com.jakob.oledoro.domain.TimerState
import com.jakob.oledoro.ui.TimerEngineHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TimerForegroundService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var notificationManager: NotificationManager
    private val engine: TimerEngine = TimerEngineHolder.engine

    private var stateObservationJob: Job? = null
    private var eventObservationJob: Job? = null
    private var isForeground = false

    inner class LocalBinder : Binder() {
        fun getService(): TimerForegroundService = this@TimerForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        startForegroundWithNotification(engine.state.value)
        observeTimerEvents()
        observeTimerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SERVICE

        when (action) {
            ACTION_START_SERVICE -> {
                engine.startTicker(serviceScope)
                startForegroundWithNotification(engine.state.value)
            }
            ACTION_SYNC -> {
                if (isForeground) {
                    val notification = notificationHelper.buildTimerNotification(engine.state.value)
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID_TIMER, notification)
                } else {
                    startForegroundWithNotification(engine.state.value)
                }
            }
            NotificationHelper.ACTION_NOTIFICATION_PAUSE -> {
                engine.pause()
            }
            NotificationHelper.ACTION_NOTIFICATION_RESUME -> {
                engine.startTicker(serviceScope)
            }
            NotificationHelper.ACTION_NOTIFICATION_NEXT_PHASE -> {
                engine.nextPhase(autoStart = true)
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_ALERT)
            }
            NotificationHelper.ACTION_NOTIFICATION_RESET -> {
                engine.reset()
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_ALERT)
            }
            ACTION_STOP_SERVICE -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
        }

        if (!isForeground) {
            startForegroundWithNotification(engine.state.value)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startForegroundWithNotification(state: TimerState) {
        val notification = notificationHelper.buildTimerNotification(state)
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID_TIMER,
            notification,
            foregroundType
        )
        isForeground = true
    }

    private fun observeTimerState() {
        stateObservationJob?.cancel()
        stateObservationJob = engine.state.onEach { state ->
            if (isForeground) {
                val notification = notificationHelper.buildTimerNotification(state)
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID_TIMER, notification)
            }
        }.launchIn(serviceScope)
    }

    private fun observeTimerEvents() {
        eventObservationJob?.cancel()
        eventObservationJob = engine.events.onEach { event ->
            when (event) {
                is TimerEvent.PhaseCompleted -> {
                    if (OledoroApp.settingsManager.autoBrightenOnFinish.value && OledoroApp.settingsManager.isDimmingActive.value) {
                        OledoroApp.settingsManager.setDimmingActive(false)
                    }
                    val alertNotification = notificationHelper.buildCompletionNotification(event.phase)
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID_ALERT, alertNotification)
                }
            }
        }.launchIn(serviceScope)
    }

    private fun stopForegroundService() {
        isForeground = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    companion object {
        const val ACTION_START_SERVICE = "com.jakob.oledoro.action.START_SERVICE"
        const val ACTION_SYNC = "com.jakob.oledoro.action.SYNC"
        const val ACTION_STOP_SERVICE = "com.jakob.oledoro.action.STOP_SERVICE"

        fun startService(context: Context, action: String = ACTION_START_SERVICE) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
