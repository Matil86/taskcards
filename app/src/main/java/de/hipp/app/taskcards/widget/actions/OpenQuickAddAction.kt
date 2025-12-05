package de.hipp.app.taskcards.widget.actions

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.hipp.app.taskcards.widget.QuickAddActivity

/**
 * Action to open quick add dialog.
 */
class OpenQuickAddAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val intent = Intent(context, QuickAddActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            context.startActivity(intent)
            Log.d(TAG, "Opening quick add dialog")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening quick add", e)
        }
    }

    companion object {
        private const val TAG = "OpenQuickAddAction"
    }
}
