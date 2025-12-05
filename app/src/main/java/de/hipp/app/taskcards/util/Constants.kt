package de.hipp.app.taskcards.util

import java.util.UUID

/**
 * Application-wide constants.
 */
object Constants {
    /**
     * Default list ID used for single-list mode (local/unauthenticated).
     * When authenticated, use generateNewListId() instead.
     */
    const val DEFAULT_LIST_ID = "default-list"

    /**
     * Generate a new unique UUID-based list ID for authenticated users.
     * Each call generates a fresh UUID to ensure lists never get mixed up.
     *
     * @return A unique UUID-based list ID
     */
    fun generateNewListId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Check if a list ID is a UUID (authenticated user list).
     *
     * @param listId The list ID to check
     * @return True if the list ID is a valid UUID format
     */
    fun isUuidListId(listId: String): Boolean {
        return try {
            UUID.fromString(listId)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
