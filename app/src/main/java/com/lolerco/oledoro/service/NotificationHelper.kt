package com.lolerco.oledoro.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lolerco.oledoro.OledoroApp
import com.lolerco.oledoro.R
import com.lolerco.oledoro.domain.TimeFormatter
import com.lolerco.oledoro.domain.TimerPhase
import com.lolerco.oledoro.domain.TimerState
import com.lolerco.oledoro.domain.TimerStatus
import com.lolerco.oledoro.ui.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val NOTIFICATION_ID_TIMER = 1001
        const val NOTIFICATION_ID_ALERT = 1002

        const val REQUEST_CODE_CONTENT = 100
        const val REQUEST_CODE_PAUSE = 101
        const val REQUEST_CODE_RESUME = 102
        const val REQUEST_CODE_NEXT = 103
        const val REQUEST_CODE_RESET = 104

        const val ACTION_NOTIFICATION_PAUSE = "com.lolerco.oledoro.action.NOTIFICATION_PAUSE"
        const val ACTION_NOTIFICATION_RESUME = "com.lolerco.oledoro.action.NOTIFICATION_RESUME"
        const val ACTION_NOTIFICATION_NEXT_PHASE = "com.lolerco.oledoro.action.NOTIFICATION_NEXT_PHASE"
        const val ACTION_NOTIFICATION_RESET = "com.lolerco.oledoro.action.NOTIFICATION_RESET"
    }

    /**
     * Builds the Promoted Rich Ongoing Notification / Live Updates action card for the active timer.
     * Uses CATEGORY_STOPWATCH, PRIORITY_DEFAULT, and FOREGROUND_SERVICE_IMMEDIATE so the
     * lockscreen action card / Samsung Now Bar renders immediately without unlock prompts.
     */
    fun buildTimerNotification(state: TimerState): Notification {
        val title = when (state.phase) {
            TimerPhase.FOCUS -> "oledoro Focus (Round ${state.currentRound}/${state.totalRounds})"
            TimerPhase.SHORT_BREAK -> "oledoro Short Break"
            TimerPhase.LONG_BREAK -> "oledoro Long Break"
        }

        val builder = NotificationCompat.Builder(context, OledoroApp.CHANNEL_LIVE_UPDATES)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(createContentPendingIntent())

        // SystemUI Chronometer or Overtime / Paused state with rich content text & BigTextStyle
        val contentText = if (state.status == TimerStatus.RUNNING) {
            val targetEpochMs = System.currentTimeMillis() + state.remainingMs
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setWhen(targetEpochMs)
            "${TimeFormatter.format(state.remainingMs)} remaining • ${state.nextPhasePreview}"
        } else if (state.isOvertime) {
            builder.setUsesChronometer(false)
            "Overtime: ${TimeFormatter.format(state.remainingMs)} • ${state.nextPhasePreview}"
        } else {
            builder.setUsesChronometer(false)
            "Paused (${TimeFormatter.format(state.remainingMs)}) • ${state.nextPhasePreview}"
        }

        builder.setContentText(contentText)
        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(contentText)
        )

        // Action 1: Pause or Resume
        if (state.status == TimerStatus.RUNNING || state.status == TimerStatus.OVERTIME) {
            builder.addAction(
                0,
                context.getString(R.string.action_pause),
                createServiceActionPendingIntent(ACTION_NOTIFICATION_PAUSE, REQUEST_CODE_PAUSE)
            )
        } else {
            builder.addAction(
                0,
                context.getString(R.string.action_resume),
                createServiceActionPendingIntent(ACTION_NOTIFICATION_RESUME, REQUEST_CODE_RESUME)
            )
        }

        // Action 2: Next Phase
        val nextLabel = if (state.phase == TimerPhase.FOCUS) {
            context.getString(R.string.action_start_break)
        } else {
            context.getString(R.string.action_start_focus)
        }
        builder.addAction(
            0,
            nextLabel,
            createServiceActionPendingIntent(ACTION_NOTIFICATION_NEXT_PHASE, REQUEST_CODE_NEXT)
        )

        // Action 3: Reset
        builder.addAction(
            0,
            context.getString(R.string.action_reset),
            createServiceActionPendingIntent(ACTION_NOTIFICATION_RESET, REQUEST_CODE_RESET)
        )

        return builder.build()
    }

    /**
     * Builds a high-priority sound/vibrate alert notification when a phase finishes.
     */
    fun buildCompletionNotification(phase: TimerPhase): Notification {
        val title = context.getString(R.string.notification_timer_finished_title)
        val message = when (phase) {
            TimerPhase.FOCUS -> context.getString(R.string.notification_timer_finished_focus)
            TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> context.getString(R.string.notification_timer_finished_break)
        }
        val nextActionLabel = if (phase == TimerPhase.FOCUS) {
            context.getString(R.string.action_start_break)
        } else {
            context.getString(R.string.action_start_focus)
        }

        return NotificationCompat.Builder(context, OledoroApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(message)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(createContentPendingIntent())
            .addAction(
                0,
                nextActionLabel,
                createServiceActionPendingIntent(ACTION_NOTIFICATION_NEXT_PHASE, REQUEST_CODE_NEXT)
            )
            .build()
    }

    private fun createContentPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, REQUEST_CODE_CONTENT, intent, flags)
    }

    private fun createServiceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
