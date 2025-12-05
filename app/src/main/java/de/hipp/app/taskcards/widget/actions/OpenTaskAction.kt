package de.hipp.app.taskcards.widget.actions

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.hipp.app.taskcards.ui.MainActivity

/**
 * Action to open the app and navigate to a specific task.
 */
class OpenTaskAction : ActionCallback {
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
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("taskId", taskId)
                putExtra("listId", listId)
            }

            context.startActivity(intent)
            Log.d(TAG, "Opening task $taskId from list $listId")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening task", e)
        }
    }

    companion object {
        private const val TAG = "OpenTaskAction"
        val TaskIdKey = ActionParameters.Key<String>("taskId")
        val ListIdKey = ActionParameters.Key<String>("listId")
    }
}
