package de.hipp.app.taskcards.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
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
import de.hipp.app.taskcards.model.DueDateStatus
import de.hipp.app.taskcards.model.TaskItem
import de.hipp.app.taskcards.model.calculateDueDateStatus
import de.hipp.app.taskcards.widget.actions.OpenConfigAction
import de.hipp.app.taskcards.widget.actions.RefreshWidgetAction
import de.hipp.app.taskcards.widget.actions.SetReminderAction
import de.hipp.app.taskcards.widget.actions.ToggleTaskAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main content composable for the TaskListWidget.
 * Displays task list header, task item, and reminder buttons.
 */
@Composable
fun TaskListWidgetContent(config: WidgetPreferences, task: TaskItem?) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(androidx.glance.R.color.glance_colorSurface))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tasks",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

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

        // Task or empty message
        if (task == null) {
            Text(
                text = "No active tasks",
                style = TextStyle(
                    fontSize = 14.sp
                )
            )
        } else {
            // Show the task
            TaskWidgetItem(task, config.listId)

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Reminder buttons
            Text(
                text = "Remind me in:",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                // 5 minute button
                ReminderButton(
                    text = "5m",
                    taskId = task.id,
                    listId = config.listId,
                    delayMinutes = 5
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                // 30 minute button
                ReminderButton(
                    text = "30m",
                    taskId = task.id,
                    listId = config.listId,
                    delayMinutes = 30
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                // 1 hour button
                ReminderButton(
                    text = "1h",
                    taskId = task.id,
                    listId = config.listId,
                    delayMinutes = 60
                )
            }
        }
    }
}

/**
 * Reminder button composable for setting quick reminders.
 */
@Composable
fun ReminderButton(
    text: String,
    taskId: String,
    listId: String,
    delayMinutes: Int
) {
    Box(
        modifier = GlanceModifier
            .background(ColorProvider(androidx.glance.R.color.glance_colorPrimaryContainer))
            .cornerRadius(8.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(
                actionRunCallback<SetReminderAction>(
                    actionParametersOf(
                        SetReminderAction.TaskIdKey to taskId,
                        SetReminderAction.ListIdKey to listId,
                        SetReminderAction.DelayMinutesKey to delayMinutes
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(androidx.glance.R.color.glance_colorOnPrimaryContainer)
            )
        )
    }
}

/**
 * Task item composable for displaying a single task in the widget.
 */
@Composable
fun TaskWidgetItem(task: TaskItem, listId: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(androidx.glance.R.color.glance_colorSurfaceVariant))
            .cornerRadius(8.dp)
            .padding(12.dp)
            .clickable(
                actionRunCallback<ToggleTaskAction>(
                    actionParametersOf(
                        ToggleTaskAction.TaskIdKey to task.id,
                        ToggleTaskAction.ListIdKey to listId
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        CheckBox(
            checked = task.done,
            onCheckedChange = actionRunCallback<ToggleTaskAction>(
                actionParametersOf(
                    ToggleTaskAction.TaskIdKey to task.id,
                    ToggleTaskAction.ListIdKey to listId
                )
            ),
            modifier = GlanceModifier.size(24.dp)
        )

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Task text and due date
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = task.text,
                style = TextStyle(
                    fontSize = 14.sp
                ),
                maxLines = 2
            )

            task.dueDate?.let { dueDate ->
                val status = calculateDueDateStatus(dueDate)
                val formattedDate = formatDueDate(dueDate)

                Text(
                    text = formattedDate,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = getDueDateColor(status)
                    )
                )
            }
        }
    }
}

/**
 * Widget configuration required message composable.
 */
@Composable
fun WidgetConfigRequired() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(androidx.glance.R.color.glance_colorSurface))
            .padding(16.dp)
            .clickable(actionRunCallback<OpenConfigAction>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tap to configure widget",
                style = TextStyle(
                    fontSize = 14.sp
                )
            )
        }
    }
}

/**
 * Format due date for display in widget.
 */
fun formatDueDate(dueDate: Long): String {
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return formatter.format(Date(dueDate))
}

/**
 * Get color for due date based on status.
 */
@Composable
fun getDueDateColor(status: DueDateStatus): ColorProvider {
    return when (status) {
        DueDateStatus.OVERDUE -> ColorProvider(
            androidx.compose.ui.graphics.Color(0xFFEF5350)
        )
        DueDateStatus.TODAY -> ColorProvider(
            androidx.compose.ui.graphics.Color(0xFFFF9800)
        )
        DueDateStatus.THIS_WEEK -> ColorProvider(
            androidx.compose.ui.graphics.Color(0xFF2196F3)
        )
        else -> ColorProvider(
            androidx.compose.ui.graphics.Color(0xFF9E9E9E)
        )
    }
}
