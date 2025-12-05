package de.hipp.app.taskcards.model

import android.net.Uri
import android.util.Log
import kotlinx.serialization.Serializable

/**
 * Represents a task that can be shared via deep links.
 * This is a transport-friendly version of TaskItem for sharing.
 */
@Serializable
data class ShareableTask(
    val text: String,
    val dueDate: Long? = null,
    val reminderType: ReminderType = ReminderType.NONE,
    val notes: String? = null
) {
    /**
     * Converts this task to a deep link URL.
     * Format: taskcards://task?text=...&dueDate=...&reminder=...&notes=...
     */
    fun toDeepLink(): String {
        val params = buildList {
            add("text=${Uri.encode(text)}")
            dueDate?.let { add("dueDate=$it") }
            if (reminderType != ReminderType.NONE) add("reminder=${reminderType.name}")
            notes?.let { add("notes=${Uri.encode(it)}") }
        }.joinToString("&")

        return "taskcards://task?$params"
    }

    companion object {
        private const val TAG = "ShareableTask"

        /**
         * Parses a deep link URI into a ShareableTask.
         * Returns null if the URI is invalid or missing required fields.
         */
        fun fromDeepLink(uri: Uri): ShareableTask? {
            if (uri.scheme != "taskcards" || uri.host != "task") {
                Log.w(TAG, "Invalid deep link scheme or host: ${uri.scheme}://${uri.host}")
                return null
            }

            val text = uri.getQueryParameter("text")
            if (text.isNullOrBlank()) {
                Log.w(TAG, "Deep link missing required 'text' parameter")
                return null
            }

            val dueDate = uri.getQueryParameter("dueDate")?.toLongOrNull()
            val reminderType = try {
                uri.getQueryParameter("reminder")
                    ?.let { ReminderType.valueOf(it) }
                    ?: ReminderType.NONE
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid reminder type in deep link", e)
                ReminderType.NONE
            }

            val notes = uri.getQueryParameter("notes")

            return ShareableTask(
                text = text,
                dueDate = dueDate,
                reminderType = reminderType,
                notes = notes
            )
        }
    }
}
