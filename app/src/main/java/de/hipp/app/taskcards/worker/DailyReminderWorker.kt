package de.hipp.app.taskcards.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.preferences.Settings
import de.hipp.app.taskcards.ui.MainActivity
import de.hipp.app.taskcards.util.Constants
import de.hipp.app.taskcards.util.LocaleHelper
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * WorkManager worker that shows daily reminder notifications.
 * Scheduled to run at the time set by the user in Settings.
 * Reminds the user about their active tasks.
 */
class DailyReminderWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()
    private val taskListRepository: TaskListRepository by inject()

    companion object {
        private const val TAG = "DailyReminderWorker"
        const val WORK_NAME = "daily_reminder_work"
        private const val NOTIFICATION_ID = 1000 // Unique ID for daily reminders
    }

    override suspend fun doWork(): Result {
        return try {
            val currentTime = java.util.Calendar.getInstance()
            val timeStr = String.format(
                java.util.Locale.ROOT,
                "%02d:%02d:%02d",
                currentTime.get(java.util.Calendar.HOUR_OF_DAY),
                currentTime.get(java.util.Calendar.MINUTE),
                currentTime.get(java.util.Calendar.SECOND)
            )
            Log.d(TAG, "Running daily reminder check at $timeStr")
            Log.d(TAG, "Worker execution timestamp: ${System.currentTimeMillis()}")

            // Get user preferences
            val settings = preferencesRepository.settings.first()

            // Wrap context with user's language preference for localized notifications
            val localizedContext = LocaleHelper.setLocale(context, settings.language)

            // Check if reminders are enabled
            if (!settings.remindersEnabled) {
                Log.d(TAG, "Reminders disabled, skipping notification")
                return Result.success()
            }

            // Get list ID and active tasks
            val listId = preferencesRepository.getLastUsedListId() ?: Constants.DEFAULT_LIST_ID

            // Get active tasks count
            val tasks = taskListRepository.observeTasks(listId).first()
            val activeTasks = tasks.filter { !it.done && !it.removed }

            if (activeTasks.isEmpty()) {
                Log.d(TAG, "No active tasks, skipping notification")
                return Result.success()
            }

            // Show the notification with localized context
            showDailyReminder(localizedContext, activeTasks.size, settings)
            Log.d(TAG, "Daily reminder notification shown for ${activeTasks.size} tasks")

            Result.success()
        } catch (e: SecurityException) {
            // Permanent failure: missing notification permission cannot be fixed by retrying
            Log.e(TAG, "Permission denied showing daily reminder notification", e)
            Result.failure()
        } catch (e: Exception) {
            // Transient failure (e.g. repository or network unavailable): retry with backoff
            Log.e(TAG, "Transient error showing daily reminder notification, will retry", e)
            Result.retry()
        }
    }

    private fun showDailyReminder(context: Context, taskCount: Int, settings: Settings) {
        // Create intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use a simple motivational message without mentioning the exact count
        val message = context.getString(R.string.daily_reminder_message)

        // Build the notification with user preferences
        val notificationBuilder = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.daily_reminder_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Apply sound preference
        if (!settings.notificationSound) {
            notificationBuilder.setSilent(true)
        } else {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND)
        }

        // Apply vibration preference
        if (!settings.notificationVibration) {
            notificationBuilder.setVibrate(longArrayOf(0)) // No vibration
        } else {
            // Use default vibration pattern (0ms delay, 500ms vibrate, 200ms pause, 500ms vibrate)
            notificationBuilder.setVibrate(longArrayOf(0, 500, 200, 500))
        }

        // Show the notification
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notificationBuilder.build())
            Log.d(TAG, "Daily reminder notification displayed")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for showing notification", e)
        }
    }
}
