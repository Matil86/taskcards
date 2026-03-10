package de.hipp.app.taskcards.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import de.hipp.app.taskcards.data.preferences.Settings
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.TaskItem
import java.util.Calendar

/**
 * Manages scheduling and cancellation of task reminder notifications using AlarmManager exact alarms.
 * Reminders are scheduled based on the task's due date and reminder type.
 */
class ReminderScheduler(
    private val context: Context
) {
    companion object {
        private const val TAG = "ReminderScheduler"
    }

    /**
     * Schedules a reminder notification for a task using an exact alarm.
     * If a reminder already exists for this task, it will be replaced.
     *
     * @param task The task to schedule a reminder for
     * @param settings User settings containing reminder time preferences
     */
    fun scheduleReminder(task: TaskItem, settings: Settings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted, skipping")
            return
        }

        // Don't schedule if reminders are disabled globally
        if (!settings.remindersEnabled) {
            Log.d(TAG, "Reminders disabled, skipping schedule for task ${task.id}")
            return
        }

        // Don't schedule if task has no due date or reminder type is NONE
        if (task.dueDate == null || task.reminderType == ReminderType.NONE) {
            Log.d(TAG, "Task ${task.id} has no due date or reminder type NONE, skipping")
            return
        }

        val reminderTime = calculateReminderTime(task.dueDate, task.reminderType, settings)

        // Don't schedule past reminders
        val now = System.currentTimeMillis()
        if (reminderTime < now) {
            Log.d(TAG, "Reminder time is in the past for task ${task.id}, skipping")
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER
            putExtra(ReminderAlarmReceiver.KEY_TASK_ID, task.id)
            putExtra(ReminderAlarmReceiver.KEY_LIST_ID, task.listId)
            putExtra(ReminderAlarmReceiver.KEY_TASK_TEXT, task.text)
            putExtra(ReminderAlarmReceiver.KEY_DUE_DATE, task.dueDate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )

        Log.d(TAG, "Scheduled exact alarm for task ${task.id} at $reminderTime")
    }

    /**
     * Cancels a scheduled reminder for a task.
     *
     * @param taskId The ID of the task whose reminder should be cancelled
     */
    fun cancelReminder(taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
        Log.d(TAG, "Cancelled exact alarm for task $taskId")
    }

    /**
     * Calculates the exact timestamp when a reminder should be shown.
     * Based on the due date, reminder type, and user's preferred reminder time.
     *
     * @param dueDate The task's due date timestamp
     * @param reminderType When to remind (on due date, 1 day before, 1 week before)
     * @param settings User settings containing reminder hour and minute
     * @return Timestamp in milliseconds when the reminder should be shown
     */
    private fun calculateReminderTime(
        dueDate: Long,
        reminderType: ReminderType,
        settings: Settings
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dueDate

        // Set time to the user's preferred reminder time
        calendar.set(Calendar.HOUR_OF_DAY, settings.reminderHour)
        calendar.set(Calendar.MINUTE, settings.reminderMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Adjust date based on reminder type
        when (reminderType) {
            ReminderType.ON_DUE_DATE -> {
                // Reminder on the due date at the configured time
            }
            ReminderType.ONE_DAY_BEFORE -> {
                // Reminder 1 day before the due date
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            ReminderType.ONE_WEEK_BEFORE -> {
                // Reminder 1 week (7 days) before the due date
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            ReminderType.NONE -> {
                // Should not reach here, but handle gracefully
                Log.w(TAG, "calculateReminderTime called with ReminderType.NONE")
            }
        }

        return calendar.timeInMillis
    }
}
