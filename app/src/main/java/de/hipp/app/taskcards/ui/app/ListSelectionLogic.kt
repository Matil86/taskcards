package de.hipp.app.taskcards.ui.app

import de.hipp.app.taskcards.model.TaskList

/**
 * Selects the appropriate list ID from available lists and stored preference.
 *
 * @param lists All lists the user has access to
 * @param storedId The last-used list ID from preferences (may be null or invalid)
 * @return The selected list ID, or null if no lists exist (caller should create one)
 */
fun selectDefaultList(lists: List<TaskList>, storedId: String?): String? {
    return when {
        lists.isEmpty() -> null
        lists.size == 1 -> lists.first().id
        else -> {
            // 2+ lists: use stored preference if valid, otherwise most recently modified
            val stored = lists.firstOrNull { it.id == storedId }
            stored?.id ?: lists.maxByOrNull { it.lastModifiedAt }?.id
        }
    }
}
