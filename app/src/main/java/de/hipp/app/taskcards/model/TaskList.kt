package de.hipp.app.taskcards.model

/**
 * Represents a task list with metadata.
 * Each list contains multiple TaskItems.
 */
data class TaskList(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis()
)
