package de.hipp.app.taskcards.worker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BroadcastReceiver that fires when an exact alarm triggers for a task reminder.
 * Shows the reminder notification to the user.
 */
class ReminderAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val KEY_TASK_ID = "taskId"
        const val KEY_LIST_ID = "listId"
        const val KEY_TASK_TEXT = "taskText"
        const val KEY_DUE_DATE = "dueDate"
        const val ACTION_REMINDER = "de.hipp.app.taskcards.REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(KEY_TASK_ID) ?: return
        val listId = intent.getStringExtra(KEY_LIST_ID) ?: return
        val taskText = intent.getStringExtra(KEY_TASK_TEXT) ?: return
        val dueDate = intent.getLongExtra(KEY_DUE_DATE, 0L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                showNotification(context, taskId, listId, taskText, dueDate)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showNotification(context: Context, taskId: String, listId: String, taskText: String, dueDate: Long) {
        val settings = try {
            preferencesRepository.settings.first()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading preferences, using defaults", e)
            null
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dueDateText = dateFormat.format(Date(dueDate))

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

        if (settings != null) {
            if (!settings.notificationSound) {
                notificationBuilder.setSilent(true)
            }
        }

        val notification = notificationBuilder.build()

        try {
            NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
            Log.d(TAG, "Showed reminder notification for task $taskId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for showing notification", e)
        }
    }
}
