package de.hipp.app.taskcards.widget.actions

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import de.hipp.app.taskcards.widget.WidgetConfigActivity

/**
 * Action to open widget configuration screen.
 */
class OpenConfigAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val intent = Intent(context, WidgetConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // Get the widget ID from GlanceId
                val manager = GlanceAppWidgetManager(context)
                val appWidgetId = manager.getAppWidgetId(glanceId)
                putExtra("appWidgetId", appWidgetId)
            }

            context.startActivity(intent)
            Log.d(TAG, "Opening widget configuration")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening configuration", e)
        }
    }

    companion object {
        private const val TAG = "OpenConfigAction"
    }
}
