package de.hipp.app.taskcards.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for task operations.
 * Provides database queries for CRUD operations on tasks.
 */
@Dao
interface TaskDao {
    /**
     * Observe all tasks for a specific list.
     * Results are sorted by removed status (active first), then by order.
     */
    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY removed ASC, `order` ASC")
    fun observeTasks(listId: String): Flow<List<TaskEntity>>

    /**
     * Get all tasks for a specific list (one-shot query).
     */
    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY removed ASC, `order` ASC")
    suspend fun getTasks(listId: String): List<TaskEntity>

    /**
     * Get a specific task by ID.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    /**
     * Insert a new task.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    /**
     * Update an existing task.
     */
    @Update
    suspend fun updateTask(task: TaskEntity)

    /**
     * Update multiple tasks at once (for reordering).
     */
    @Update
    suspend fun updateTasks(tasks: List<TaskEntity>)

    /**
     * Delete a task by ID.
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    /**
     * Delete all tasks for a specific list.
     */
    @Query("DELETE FROM tasks WHERE listId = :listId")
    suspend fun clearList(listId: String)

    /**
     * Get count of distinct lists in the database.
     */
    @Query("SELECT COUNT(DISTINCT listId) FROM tasks")
    suspend fun getActiveListCount(): Int

    /**
     * Get the minimum order value for non-removed tasks in a list.
     * Used to calculate order for new tasks.
     */
    @Query("SELECT MIN(`order`) FROM tasks WHERE listId = :listId AND removed = 0")
    suspend fun getMinOrder(listId: String): Int?
}
