package de.hipp.app.taskcards.widget.actions

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.model.TaskItem
import de.hipp.app.taskcards.widget.TaskListWidget
import kotlinx.coroutines.flow.first

/**
 * Action to toggle a task's completion status from a widget.
 */
class ToggleTaskAction : ActionCallback {
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

        try {
            // Get repository from RepositoryProvider
            val repo: TaskListRepository = de.hipp.app.taskcards.di.RepositoryProvider.getRepository(context)

            // Find the task and toggle its done status
            val tasks: List<TaskItem> = repo.observeTasks(listId).first()
            val task: TaskItem? = tasks.find { it.id == taskId }

            if (task != null) {
                repo.markDone(listId, taskId, !task.done)
                Log.d(TAG, "Toggled task $taskId to ${!task.done}")

                // Update widget to reflect changes
                TaskListWidget().update(context, glanceId)
            } else {
                Log.w(TAG, "Task $taskId not found in list $listId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling task", e)
        }
    }

    companion object {
        private const val TAG = "ToggleTaskAction"
        val TaskIdKey = ActionParameters.Key<String>("taskId")
        val ListIdKey = ActionParameters.Key<String>("listId")
    }
}
