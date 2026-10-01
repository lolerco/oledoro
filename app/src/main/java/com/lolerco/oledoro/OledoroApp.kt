package com.lolerco.oledoro

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.lolerco.oledoro.data.AppSettingsManager
import com.lolerco.oledoro.domain.TimerEngine
import com.lolerco.oledoro.ui.TimerEngineHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class OledoroApp : Application() {

    companion object {
        const val CHANNEL_LIVE_UPDATES = "oledoro_live_updates_v6"
        const val CHANNEL_ALERTS = "oledoro_alerts"

        lateinit var instance: OledoroApp
            private set

        val settingsManager: AppSettingsManager by lazy {
            AppSettingsManager(instance)
        }

        fun getTimerEngine(): TimerEngine = TimerEngineHolder.engine
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Live updates channel: IMPORTANCE_HIGH ensures Android Lock Screen / Samsung Now Bar
            // promotes the action card, while setSound(null, null) keeps it silent while ticking.
            val liveUpdatesChannel = NotificationChannel(
                CHANNEL_LIVE_UPDATES,
                getString(R.string.channel_live_updates_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_live_updates_description)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Alerts channel: High importance, sound and vibration for phase completed
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_alerts_description)
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC

                val alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
                setSound(alertSoundUri, audioAttributes)
            }

            notificationManager.createNotificationChannels(listOf(liveUpdatesChannel, alertsChannel))
        }
    }
}
