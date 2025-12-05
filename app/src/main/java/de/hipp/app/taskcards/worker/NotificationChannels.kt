package de.hipp.app.taskcards.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Creates and manages notification channels for the app.
 * Channels are required for Android 8.0 (API 26) and above.
 */
object NotificationChannels {
    const val REMINDERS_CHANNEL_ID = "task_reminders"
    const val REMINDERS_CHANNEL_NAME = "Task Reminders"
    const val REMINDERS_CHANNEL_DESCRIPTION = "Notifications for upcoming task due dates"

    /**
     * Creates all notification channels used by the app.
     * Safe to call multiple times - channels will only be created once.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDERS_CHANNEL_ID,
                REMINDERS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = REMINDERS_CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
