package de.hipp.app.taskcards.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import de.hipp.app.taskcards.data.AppDatabase
import de.hipp.app.taskcards.data.RoomTaskListRepository
import kotlinx.coroutines.flow.first

/**
 * Widget that displays the highest priority active task from a specific task list.
 * Shows 1 task with a checkbox and reminder buttons (5m, 30m, 1h).
 */
class TaskListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = WidgetPreferencesRepository(context)
        val manager = GlanceAppWidgetManager(context)
        val widgetId = manager.getAppWidgetId(id)
        val config = prefs.getWidgetConfig(widgetId)

        // Fetch task data in suspend context before rendering
        val task = if (config != null) {
            try {
                val database = AppDatabase.getInstance(context)
                val repo = RoomTaskListRepository(database.taskDao(), null, null)
                repo.observeTasks(config.listId).first()
                    .filter { !it.done && !it.removed }
                    .sortedBy { it.order }
                    .firstOrNull()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tasks", e)
                null
            }
        } else null

        provideContent {
            GlanceTheme {
                if (config == null) {
                    WidgetConfigRequired()
                } else {
                    TaskListWidgetContent(config, task)
                }
            }
        }
    }

    companion object {
        private const val TAG = "TaskListWidget"
    }
}
