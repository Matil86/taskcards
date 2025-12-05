package de.hipp.app.taskcards.model

import java.util.UUID

/**
 * Represents one task in a collaborative list identified by [listId].
 * order: lower value means higher priority.
 * dueDate: timestamp in milliseconds, null if no due date set.
 * reminderType: type of reminder to schedule for this task.
 */
 data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val text: String,
    val order: Int,
    val done: Boolean = false,
    val removed: Boolean = false,
    val dueDate: Long? = null,
    val reminderType: ReminderType = ReminderType.NONE,
 )
