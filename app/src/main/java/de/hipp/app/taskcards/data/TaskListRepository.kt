package de.hipp.app.taskcards.data

import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.TaskItem
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for task list storage. Later, a Firestore implementation can be plugged in.
 */
interface TaskListRepository {
    companion object {
        const val MAX_TASK_TEXT_LENGTH = 500
    }

    fun observeTasks(listId: String): Flow<List<TaskItem>>

    suspend fun addTask(listId: String, text: String): TaskItem

    suspend fun removeTask(listId: String, taskId: String)

    suspend fun restoreTask(listId: String, taskId: String)

    suspend fun markDone(listId: String, taskId: String, done: Boolean)

    /**
     * Reorder by moving [taskId] to [toIndex] (0-based) among non-removed tasks,
     * adjusting order so lower order means higher priority.
     */
    suspend fun moveTask(listId: String, taskId: String, toIndex: Int)

    /**
     * Updates the due date and reminder type for a task.
     * @param listId the list containing the task
     * @param taskId the task to update
     * @param dueDate timestamp in milliseconds, or null to clear due date
     * @param reminderType type of reminder to schedule
     * @throws IllegalArgumentException if dueDate is in the past (when not null)
     */
    suspend fun updateTaskDueDate(
        listId: String,
        taskId: String,
        dueDate: Long?,
        reminderType: ReminderType
    )

    /**
     * Clear all tasks from a specific list.
     * Use this to free memory when a list is no longer needed.
     */
    suspend fun clearList(listId: String)

    /**
     * Get count of lists currently stored in memory.
     * Useful for debugging memory usage.
     */
    suspend fun getActiveListCount(): Int
}
