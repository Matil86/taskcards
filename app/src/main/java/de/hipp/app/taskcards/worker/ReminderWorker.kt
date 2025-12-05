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
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepositoryImpl
import de.hipp.app.taskcards.ui.MainActivity
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WorkManager worker that shows notifications for task reminders.
 * Scheduled by ReminderScheduler when a task has a due date and reminder set.
 */
class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ReminderWorker"
        const val KEY_TASK_ID = "taskId"
        const val KEY_LIST_ID = "listId"
        const val KEY_TASK_TEXT = "taskText"
        const val KEY_DUE_DATE = "dueDate"
    }

    override suspend fun doWork(): Result {
        return try {
            val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
            val listId = inputData.getString(KEY_LIST_ID) ?: return Result.failure()
            val taskText = inputData.getString(KEY_TASK_TEXT) ?: return Result.failure()
            val dueDate = inputData.getLong(KEY_DUE_DATE, 0L)

            if (dueDate == 0L) {
                Log.w(TAG, "Invalid due date for task $taskId")
                return Result.failure()
            }

            showNotification(taskId, listId, taskText, dueDate)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing reminder notification", e)
            Result.failure()
        }
    }

    private suspend fun showNotification(taskId: String, listId: String, taskText: String, dueDate: Long) {
        // Get user preferences for notification settings
        val preferencesRepo = PreferencesRepositoryImpl(context)
        val settings = try {
            preferencesRepo.settings.first()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading preferences, using defaults", e)
            null
        }

        // Format the due date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dueDateText = dateFormat.format(Date(dueDate))

        // Create intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("listId", listId)
            putExtra("taskId", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification with user preferences
        val notificationBuilder = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Due: $dueDateText")
            .setContentText(taskText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(taskText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Apply sound and vibration preferences
        if (settings != null) {
            if (!settings.notificationSound) {
                notificationBuilder.setSilent(true)
            }
            // Note: Vibration is controlled at the channel level for Android O+
            // For pre-O devices, NotificationCompat handles it automatically based on channel settings
        }

        val notification = notificationBuilder.build()

        // Show the notification
        try {
            NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
            Log.d(TAG, "Showed reminder notification for task $taskId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for showing notification", e)
        }
    }
}
