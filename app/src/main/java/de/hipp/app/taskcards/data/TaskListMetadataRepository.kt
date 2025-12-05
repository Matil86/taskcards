package de.hipp.app.taskcards.data

import de.hipp.app.taskcards.model.TaskList
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing task list metadata.
 * Provides CRUD operations for TaskList objects.
 */
interface TaskListMetadataRepository {
    /**
     * Observe all task lists.
     * Returns a Flow that emits the list of all TaskLists sorted by creation time.
     */
    fun observeTaskLists(): Flow<List<TaskList>>

    /**
     * Create a new task list with the given name.
     * @param name The name of the new list
     * @return The ID of the newly created list
     */
    suspend fun createList(name: String): String

    /**
     * Rename an existing task list.
     * @param listId The ID of the list to rename
     * @param newName The new name for the list
     */
    suspend fun renameList(listId: String, newName: String)

    /**
     * Delete a task list and all its tasks.
     * @param listId The ID of the list to delete
     */
    suspend fun deleteList(listId: String)

    /**
     * Get the ID of the default list (the first list, or null if none exist).
     * Used for migration and initial app setup.
     * @return The list ID, or null if no lists exist
     */
    suspend fun getDefaultListId(): String?
}
