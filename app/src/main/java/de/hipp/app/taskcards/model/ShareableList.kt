package de.hipp.app.taskcards.model

import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Represents a complete task list that can be shared via deep links.
 * Contains the list name and all tasks to be imported.
 */
@Serializable
data class ShareableList(
    val name: String,
    val tasks: List<ShareableTask>
) {
    /**
     * Converts this list to a deep link URL.
     * The task data is serialized to JSON and base64-encoded to handle special characters.
     * Format: taskcards://list?name=...&data=<base64-encoded-json>
     */
    fun toDeepLink(): String {
        val json = Json.encodeToString(this)
        val base64 = Base64.encodeToString(
            json.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "taskcards://list?name=${Uri.encode(name)}&data=$base64"
    }

    companion object {
        private const val TAG = "ShareableList"

        /**
         * Parses a deep link URI into a ShareableList.
         * Returns null if the URI is invalid or data cannot be decoded.
         */
        fun fromDeepLink(uri: Uri): ShareableList? {
            if (uri.scheme != "taskcards" || uri.host != "list") {
                Log.w(TAG, "Invalid deep link scheme or host: ${uri.scheme}://${uri.host}")
                return null
            }

            val name = uri.getQueryParameter("name")
            if (name.isNullOrBlank()) {
                Log.w(TAG, "Deep link missing required 'name' parameter")
                return null
            }

            val base64 = uri.getQueryParameter("data")
            if (base64.isNullOrBlank()) {
                Log.w(TAG, "Deep link missing required 'data' parameter")
                return null
            }

            return try {
                val json = String(Base64.decode(base64, Base64.URL_SAFE or Base64.NO_PADDING))
                Json.decodeFromString<ShareableList>(json)
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding deep link data", e)
                null
            }
        }
    }
}
