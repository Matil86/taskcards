package de.hipp.app.taskcards.model

/**
 * Represents a task list with metadata.
 * Each list contains multiple TaskItems.
 *
 * [creatorUid] is the UID of the user who created the list.
 * [contributors] is the array of UIDs that have access to this list; enables
 * whereArrayContains queries against the canonical /lists/{listId} document.
 */
data class TaskList(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val creatorUid: String = "",
    val contributors: List<String> = emptyList()
)
