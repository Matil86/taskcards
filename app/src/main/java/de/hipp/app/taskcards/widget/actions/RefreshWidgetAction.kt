package de.hipp.app.taskcards.widget.actions

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.hipp.app.taskcards.widget.TaskListWidget

/**
 * Action to refresh a widget manually.
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            Log.d(TAG, "Refreshing widget $glanceId")
            TaskListWidget().update(context, glanceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing widget", e)
        }
    }

    companion object {
        private const val TAG = "RefreshWidgetAction"
    }
}
