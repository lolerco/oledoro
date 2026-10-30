package com.lolerco.oledoro.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.lolerco.oledoro.OledoroApp
import com.lolerco.oledoro.domain.TimerEngine
import com.lolerco.oledoro.domain.TimerEvent
import com.lolerco.oledoro.domain.TimerPhase
import com.lolerco.oledoro.domain.TimerState
import com.lolerco.oledoro.domain.TimerStatus
import com.lolerco.oledoro.ui.TimerEngineHolder
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
                if (OledoroApp.settingsManager.autoBrightenOnFinish.value && engine.state.value.status == TimerStatus.IDLE) {
                    OledoroApp.settingsManager.setDimmingActive(true)
                }
                engine.startTicker(serviceScope)
                startForegroundWithNotification(engine.state.value)
            }
            ACTION_SYNC -> {
                engine.syncTime()
                if (isForeground) {
                    val notification = notificationHelper.buildTimerNotification(engine.state.value)
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID_TIMER, notification)
                } else {
                    startForegroundWithNotification(engine.state.value)
                }
            }
            ACTION_TIMER_EXPIRED -> {
                engine.syncTime()
            }
            NotificationHelper.ACTION_NOTIFICATION_PAUSE -> {
                engine.pause()
            }
            NotificationHelper.ACTION_NOTIFICATION_RESUME -> {
                engine.startTicker(serviceScope)
            }
            NotificationHelper.ACTION_NOTIFICATION_NEXT_PHASE -> {
                if (OledoroApp.settingsManager.autoBrightenOnFinish.value) {
                    OledoroApp.settingsManager.setDimmingActive(true)
                }
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

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        engine.pause()
        stopForegroundService()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCompletionAlarm()
        TimerEngineHolder.statsRepo.flush()
        isForeground = false
        stateObservationJob?.cancel()
        eventObservationJob?.cancel()
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        if (::notificationManager.isInitialized) {
            notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_TIMER)
            notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_ALERT)
        }
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

    private var lastNotifiedPhase: TimerPhase? = null
    private var lastNotifiedStatus: TimerStatus? = null
    private var lastNotifiedIsOvertime: Boolean? = null
    private var lastNotificationPostTimeMs: Long = 0L
    private var lastScheduledAlarmEpochMs: Long = 0L

    private fun observeTimerState() {
        stateObservationJob?.cancel()
        stateObservationJob = engine.state.onEach { state ->
            if (!isForeground) return@onEach

            // Manage AlarmManager wakeup so the completion alert fires accurately
            // without keeping the CPU awake or draining battery during sleep.
            if (state.status == TimerStatus.RUNNING && state.remainingMs > 0L) {
                val targetEpochMs = System.currentTimeMillis() + state.remainingMs
                if (kotlin.math.abs(targetEpochMs - lastScheduledAlarmEpochMs) > 2000L) {
                    lastScheduledAlarmEpochMs = targetEpochMs
                    scheduleCompletionAlarm(state.remainingMs)
                }
            } else {
                if (lastScheduledAlarmEpochMs != 0L) {
                    lastScheduledAlarmEpochMs = 0L
                    cancelCompletionAlarm()
                }
            }

            val statusChanged = state.status != lastNotifiedStatus
            val phaseChanged = state.phase != lastNotifiedPhase
            val overtimeChanged = state.isOvertime != lastNotifiedIsOvertime
            val now = System.currentTimeMillis()

            // When actively running a countdown, Android SystemUI chronometer counts down
            // natively on the display hardware without app wakeups or IPC calls.
            // We only re-post the notification when:
            // 1) Status or Phase changes (start, pause, resume, new phase)
            // 2) Transition into / out of overtime
            // 3) While in overtime, throttled to at most once every 10 seconds
            // 4) Heartbeat once every 60 seconds
            val shouldNotify = statusChanged || phaseChanged || overtimeChanged ||
                    (state.isOvertime && (now - lastNotificationPostTimeMs >= 10_000L)) ||
                    (now - lastNotificationPostTimeMs >= 60_000L)

            if (shouldNotify) {
                lastNotifiedStatus = state.status
                lastNotifiedPhase = state.phase
                lastNotifiedIsOvertime = state.isOvertime
                lastNotificationPostTimeMs = now
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
                    TimerEngineHolder.statsRepo.flush()
                    if (OledoroApp.settingsManager.autoBrightenOnFinish.value) {
                        OledoroApp.settingsManager.setDimmingActive(false)
                    }
                    val alertNotification = notificationHelper.buildCompletionNotification(event.phase)
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID_ALERT, alertNotification)
                }
            }
        }.launchIn(serviceScope)
    }

    private fun stopForegroundService() {
        cancelCompletionAlarm()
        isForeground = false
        stateObservationJob?.cancel()
        eventObservationJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        if (::notificationManager.isInitialized) {
            notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_TIMER)
            notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_ALERT)
        }
        stopSelf()
    }

    private fun scheduleCompletionAlarm(remainingMs: Long) {
        if (remainingMs <= 0L) {
            cancelCompletionAlarm()
            return
        }
        val triggerAtMillis = System.currentTimeMillis() + remainingMs
        val intent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_TIMER_EXPIRED
        }
        val pendingIntent = PendingIntent.getService(
            this,
            NotificationHelper.REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    private fun cancelCompletionAlarm() {
        val intent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_TIMER_EXPIRED
        }
        val pendingIntent = PendingIntent.getService(
            this,
            NotificationHelper.REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}
    }

    companion object {
        const val ACTION_START_SERVICE = "com.lolerco.oledoro.action.START_SERVICE"
        const val ACTION_SYNC = "com.lolerco.oledoro.action.SYNC"
        const val ACTION_STOP_SERVICE = "com.lolerco.oledoro.action.STOP_SERVICE"
        const val ACTION_TIMER_EXPIRED = "com.lolerco.oledoro.action.TIMER_EXPIRED"

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
