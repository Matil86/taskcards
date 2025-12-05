package de.hipp.app.taskcards.worker

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import de.hipp.app.taskcards.data.preferences.Settings
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.TaskItem
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Manages scheduling and cancellation of task reminder notifications using WorkManager.
 * Reminders are scheduled based on the task's due date and reminder type.
 */
class ReminderScheduler(
    private val context: Context
) {
    companion object {
        private const val TAG = "ReminderScheduler"
        private const val WORK_NAME_PREFIX = "reminder_"
    }

    /**
     * Schedules a reminder notification for a task.
     * If a reminder already exists for this task, it will be replaced.
     *
     * @param task The task to schedule a reminder for
     * @param settings User settings containing reminder time preferences
     */
    fun scheduleReminder(task: TaskItem, settings: Settings) {
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

        val delay = reminderTime - now

        val inputData = Data.Builder()
            .putString(ReminderWorker.KEY_TASK_ID, task.id)
            .putString(ReminderWorker.KEY_LIST_ID, task.listId)
            .putString(ReminderWorker.KEY_TASK_TEXT, task.text)
            .putLong(ReminderWorker.KEY_DUE_DATE, task.dueDate)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(getWorkTag(task.id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            getWorkName(task.id),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Scheduled reminder for task ${task.id} at $reminderTime (delay: ${delay}ms)")
    }

    /**
     * Cancels a scheduled reminder for a task.
     *
     * @param taskId The ID of the task whose reminder should be cancelled
     */
    fun cancelReminder(taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(getWorkName(taskId))
        Log.d(TAG, "Cancelled reminder for task $taskId")
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

    private fun getWorkName(taskId: String): String = "$WORK_NAME_PREFIX$taskId"
    private fun getWorkTag(taskId: String): String = "reminder_tag_$taskId"
}
