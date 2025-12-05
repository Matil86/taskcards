package de.hipp.app.taskcards.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.data.AppDatabase
import de.hipp.app.taskcards.data.RoomTaskListRepository
import de.hipp.app.taskcards.model.DueDateStatus
import de.hipp.app.taskcards.model.calculateDueDateStatus
import de.hipp.app.taskcards.widget.actions.RefreshWidgetAction
import kotlinx.coroutines.flow.first

/**
 * Widget that shows only tasks due today, with a badge showing overdue count.
 */
class DueTodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = WidgetPreferencesRepository(context)
        val manager = GlanceAppWidgetManager(context)
        val widgetId = manager.getAppWidgetId(id)
        val config = prefs.getWidgetConfig(widgetId)

        // Fetch task data in suspend context before rendering
        val (todayTasks, overdueCount) = if (config != null) {
            try {
                val database = AppDatabase.getInstance(context)
                val repo = RoomTaskListRepository(database.taskDao(), null, null)
                val allTasks = repo.observeTasks(config.listId).first()
                    .filter { !it.done && !it.removed }

                val today = allTasks.filter { task ->
                    task.dueDate?.let { calculateDueDateStatus(it) == DueDateStatus.TODAY } ?: false
                }.sortedBy { it.order }.take(5)

                val overdue = allTasks.count { task ->
                    task.dueDate?.let { calculateDueDateStatus(it) == DueDateStatus.OVERDUE } ?: false
                }

                Pair(today, overdue)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tasks", e)
                Pair(emptyList(), 0)
            }
        } else Pair(emptyList(), 0)

        provideContent {
            GlanceTheme {
                if (config == null) {
                    WidgetConfigRequired()
                } else {
                    DueTodayWidgetContent(config, todayTasks, overdueCount)
                }
            }
        }
    }

    companion object {
        private const val TAG = "DueTodayWidget"
    }
}

@Composable
fun DueTodayWidgetContent(
    config: WidgetPreferences,
    todayTasks: List<de.hipp.app.taskcards.model.TaskItem>,
    overdueCount: Int
) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(androidx.glance.R.color.glance_colorSurface))
            .padding(16.dp)
    ) {
        // Header with overdue badge
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Due Today",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            if (overdueCount > 0) {
                Spacer(modifier = GlanceModifier.width(8.dp))
                OverdueBadge(overdueCount)
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Refresh button
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = context.getString(R.string.cd_refresh_widget),
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionRunCallback<RefreshWidgetAction>())
            )
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Task list
        if (todayTasks.isEmpty()) {
            Text(
                text = if (overdueCount > 0) {
                    "No tasks due today (but $overdueCount overdue)"
                } else {
                    "No tasks due today"
                },
                style = TextStyle(
                    fontSize = 14.sp
                )
            )
        } else {
            todayTasks.forEach { task ->
                TaskWidgetItem(task, config.listId)
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }
}

@Composable
fun OverdueBadge(count: Int) {
    Box(
        modifier = GlanceModifier
            .size(24.dp)
            .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFFEF5350)))
            .cornerRadius(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                fontWeight = FontWeight.Bold
            )
        )
    }
}
