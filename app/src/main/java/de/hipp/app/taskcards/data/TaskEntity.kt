package de.hipp.app.taskcards.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.TaskItem
import java.util.UUID

/**
 * Room database entity representing a task.
 * Matches TaskItem fields but with Room annotations for persistence.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val text: String,
    val order: Int,
    val done: Boolean = false,
    val removed: Boolean = false,
    val dueDate: Long? = null,
    val reminderType: String = ReminderType.NONE.name,
) {
    /**
     * Convert Room entity to domain model.
     */
    fun toTaskItem(): TaskItem = TaskItem(
        id = id,
        listId = listId,
        text = text,
        order = order,
        done = done,
        removed = removed,
        dueDate = dueDate,
        reminderType = try {
            ReminderType.valueOf(reminderType)
        } catch (e: IllegalArgumentException) {
            ReminderType.NONE
        }
    )

    companion object {
        /**
         * Convert domain model to Room entity.
         */
        fun fromTaskItem(item: TaskItem): TaskEntity = TaskEntity(
            id = item.id,
            listId = item.listId,
            text = item.text,
            order = item.order,
            done = item.done,
            removed = item.removed,
            dueDate = item.dueDate,
            reminderType = item.reminderType.name
        )
    }
}
