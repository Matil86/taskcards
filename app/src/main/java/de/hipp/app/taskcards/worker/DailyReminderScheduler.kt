package de.hipp.app.taskcards.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules daily reminder notifications at the time set by the user.
 * Uses WorkManager to ensure reliable delivery even if app is closed.
 */
object DailyReminderScheduler {
    private const val TAG = "DailyReminderScheduler"

    /**
     * Schedule a daily reminder at the specified time.
     * @param context Android context
     * @param hour Hour of day (0-23)
     * @param minute Minute of hour (0-59)
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        Log.d(TAG, "Scheduling daily reminder for $hour:$minute")

        // Cancel any existing daily reminder
        cancelDailyReminder(context)

        // Calculate initial delay to the target time
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If target time is in the past today, schedule for tomorrow
            if (before(currentTime)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        Log.d(TAG, "Initial delay: ${initialDelay / 1000 / 60} minutes (${initialDelay}ms)")
        val targetTimeStr = String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute)
        val targetDateStr = "${targetTime.get(Calendar.YEAR)}-${targetTime.get(Calendar.MONTH) + 1}-${targetTime.get(Calendar.DAY_OF_MONTH)}"
        Log.d(TAG, "Target time: $targetDateStr $targetTimeStr")
        val currentTimeStr = String.format(
            java.util.Locale.ROOT,
            "%02d:%02d",
            currentTime.get(Calendar.HOUR_OF_DAY),
            currentTime.get(Calendar.MINUTE)
        )
        val currentDateStr = "${currentTime.get(Calendar.YEAR)}-${currentTime.get(Calendar.MONTH) + 1}-${currentTime.get(Calendar.DAY_OF_MONTH)}"
        Log.d(TAG, "Current time: $currentDateStr $currentTimeStr")

        // Create the work request with daily repeat
        // NOTE: NO flex interval - executes as close to exact time as possible
        // Daily notification = only 1 execution per day, so battery impact is minimal
        // Precision is more important than battery optimization for user-configured time
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, // Repeat every 24 hours
            TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false) // Allow even on low battery
                    .build()
            )
            .addTag("daily_reminder")
            .build()

        // Schedule the work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update if already exists
            dailyWorkRequest
        )

        Log.d(TAG, "Daily reminder scheduled successfully")
    }

    /**
     * Cancel the daily reminder.
     */
    fun cancelDailyReminder(context: Context) {
        Log.d(TAG, "Cancelling daily reminder")
        WorkManager.getInstance(context).cancelUniqueWork(DailyReminderWorker.WORK_NAME)
    }

    /**
     * Check if daily reminder is currently scheduled.
     */
    suspend fun isDailyReminderScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(DailyReminderWorker.WORK_NAME)
            .get() // This is a blocking call in a suspend function

        return workInfos.any { workInfo ->
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        }
    }
}
