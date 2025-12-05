package de.hipp.app.taskcards.widget.actions

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.widget.TaskListWidget
import kotlinx.coroutines.flow.first

/**
 * Action to set a reminder for a task at a specified delay from now.
 */
class SetReminderAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: run {
            Log.e(TAG, "Missing taskId parameter")
            return
        }
        val listId = parameters[ListIdKey] ?: run {
            Log.e(TAG, "Missing listId parameter")
            return
        }
        val delayMinutes = parameters[DelayMinutesKey] ?: run {
            Log.e(TAG, "Missing delayMinutes parameter")
            return
        }

        try {
            val repo = de.hipp.app.taskcards.di.RepositoryProvider.getRepository(context)

            // Get the task
            val tasks = repo.observeTasks(listId).first()
            val task = tasks.find { it.id == taskId }

            if (task != null) {
                // Calculate reminder time (current time + delay)
                val reminderTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

                // Update task with ON_DUE_DATE reminder type and due date set to reminder time
                // The repository will automatically schedule the reminder
                repo.updateTaskDueDate(
                    listId = listId,
                    taskId = taskId,
                    dueDate = reminderTime,
                    reminderType = ReminderType.ON_DUE_DATE
                )

                Log.d(TAG, "Set reminder for task $taskId in $delayMinutes minutes")

                // Update widget to reflect changes
                TaskListWidget().update(context, glanceId)
            } else {
                Log.w(TAG, "Task $taskId not found in list $listId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting reminder", e)
        }
    }

    companion object {
        private const val TAG = "SetReminderAction"
        val TaskIdKey = ActionParameters.Key<String>("taskId")
        val ListIdKey = ActionParameters.Key<String>("listId")
        val DelayMinutesKey = ActionParameters.Key<Int>("delayMinutes")
    }
}
