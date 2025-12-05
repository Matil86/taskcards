package de.hipp.app.taskcards.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Worker that periodically updates all active widgets.
 * Runs every 15 minutes to refresh widget content.
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Updating all widgets")

            val glanceManager = GlanceAppWidgetManager(applicationContext)

            // Update TaskList widgets
            glanceManager.getGlanceIds(TaskListWidget::class.java).forEach { glanceId ->
                try {
                    TaskListWidget().update(applicationContext, glanceId)
                    Log.d(TAG, "Updated TaskListWidget: $glanceId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating TaskListWidget: $glanceId", e)
                }
            }

            // Update DueToday widgets
            glanceManager.getGlanceIds(DueTodayWidget::class.java).forEach { glanceId ->
                try {
                    DueTodayWidget().update(applicationContext, glanceId)
                    Log.d(TAG, "Updated DueTodayWidget: $glanceId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating DueTodayWidget: $glanceId", e)
                }
            }

            // QuickAdd widget doesn't need periodic updates

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widgets", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val WORK_NAME = "widget_updates"

        /**
         * Schedule periodic widget updates.
         * Should be called from Application.onCreate().
         */
        fun scheduleWidgetUpdates(context: Context) {
            val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15,
                TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                updateRequest
            )

            Log.d(TAG, "Scheduled widget updates")
        }

        /**
         * Trigger an immediate widget update outside of the periodic schedule.
         * Useful after task changes.
         */
        suspend fun triggerImmediateUpdate(context: Context) {
            try {
                val glanceManager = GlanceAppWidgetManager(context)

                // Update all TaskList widgets
                glanceManager.getGlanceIds(TaskListWidget::class.java).forEach { glanceId ->
                    TaskListWidget().update(context, glanceId)
                }

                // Update all DueToday widgets
                glanceManager.getGlanceIds(DueTodayWidget::class.java).forEach { glanceId ->
                    DueTodayWidget().update(context, glanceId)
                }

                Log.d(TAG, "Triggered immediate widget update")
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering immediate update", e)
            }
        }
    }
}
