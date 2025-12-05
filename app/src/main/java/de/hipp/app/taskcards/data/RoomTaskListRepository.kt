package de.hipp.app.taskcards.data

import android.util.Log
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.TaskItem
import de.hipp.app.taskcards.worker.ReminderScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Room-backed implementation of TaskListRepository.
 * Provides persistent storage using SQLite database via Room.
 *
 * @param dao The Room DAO for database operations
 * @param reminderScheduler Scheduler for task reminder notifications
 * @param preferencesRepo Repository for user preferences (notification settings)
 * @param dispatcher The dispatcher for background operations (defaults to IO for database work)
 */
class RoomTaskListRepository(
    private val dao: TaskDao,
    private val reminderScheduler: ReminderScheduler? = null,
    private val preferencesRepo: PreferencesRepository? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TaskListRepository {
    private val mutex = Mutex()

    companion object {
        private const val TAG = "RoomTaskListRepo"
    }

    override fun observeTasks(listId: String): Flow<List<TaskItem>> =
        dao.observeTasks(listId).map { entities ->
            entities.map { it.toTaskItem() }
        }

    override suspend fun addTask(listId: String, text: String): TaskItem =
        withContext(dispatcher) {
            mutex.withLock {
                val trimmedText = text.trim()
                require(trimmedText.isNotEmpty()) { "Task text cannot be empty" }
                require(trimmedText.length <= TaskListRepository.MAX_TASK_TEXT_LENGTH) {
                    "Task text cannot exceed ${TaskListRepository.MAX_TASK_TEXT_LENGTH} characters"
                }

                // Calculate new order (one less than minimum active task order)
                val minOrder = dao.getMinOrder(listId) ?: 0
                val newOrder = minOrder - 1

                val item = TaskItem(
                    listId = listId,
                    text = trimmedText,
                    order = newOrder
                )

                dao.insertTask(TaskEntity.fromTaskItem(item))
                item
            }
        }

    override suspend fun removeTask(listId: String, taskId: String) {
        updateTask(listId, taskId) { it.copy(removed = true) }
    }

    override suspend fun restoreTask(listId: String, taskId: String) {
        updateTask(listId, taskId) { it.copy(removed = false) }
    }

    override suspend fun markDone(listId: String, taskId: String, done: Boolean) {
        updateTask(listId, taskId) { it.copy(done = done) }
    }

    override suspend fun moveTask(listId: String, taskId: String, toIndex: Int) =
        withContext(dispatcher) {
            mutex.withLock {
                val allTasks = dao.getTasks(listId)
                val activeTasks = allTasks.filter { !it.removed }.sortedBy { it.order }.toMutableList()
                val movingTask = activeTasks.firstOrNull { it.id == taskId } ?: return@withLock

                val fromIndex = activeTasks.indexOf(movingTask)
                if (fromIndex == -1) return@withLock

                val boundedIndex = toIndex.coerceIn(0, activeTasks.lastIndex)

                // Reorder the list
                activeTasks.removeAt(fromIndex)
                activeTasks.add(boundedIndex, movingTask)

                // Reassign compact orders: 0, 1, 2, ... (lower = higher priority)
                val reorderedEntities = activeTasks.mapIndexed { idx, entity ->
                    entity.copy(order = idx)
                }

                // Update all reordered tasks in the database
                dao.updateTasks(reorderedEntities)
            }
        }

    override suspend fun clearList(listId: String) =
        withContext(dispatcher) {
            mutex.withLock {
                dao.clearList(listId)
            }
        }

    override suspend fun updateTaskDueDate(
        listId: String,
        taskId: String,
        dueDate: Long?,
        reminderType: ReminderType
    ) {
        withContext(dispatcher) {
            mutex.withLock {
                // Validate due date is not in the past (allow same day)
                if (dueDate != null) {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfToday = calendar.timeInMillis

                    require(dueDate >= startOfToday) {
                        "Due date cannot be in the past"
                    }
                }

                // If clearing due date, also clear reminder
                val finalReminderType = if (dueDate == null) ReminderType.NONE else reminderType

                // Update task in database
                updateTask(listId, taskId) { entity ->
                    entity.copy(
                        dueDate = dueDate,
                        reminderType = finalReminderType.name
                    )
                }

                // Get the updated task for reminder scheduling
                val task = dao.getTaskById(taskId)?.toTaskItem()

                // Schedule or cancel reminder if scheduler is available (not in tests)
                if (task != null && reminderScheduler != null && preferencesRepo != null) {
                    try {
                        if (dueDate != null && finalReminderType != ReminderType.NONE) {
                            // Get user settings for scheduling
                            val settings = preferencesRepo.settings.first()
                            reminderScheduler.scheduleReminder(task, settings)
                            Log.d(TAG, "Scheduled reminder for task $taskId")
                        } else {
                            // Cancel reminder if due date cleared or reminder type is NONE
                            reminderScheduler.cancelReminder(taskId)
                            Log.d(TAG, "Cancelled reminder for task $taskId")
                        }
                    } catch (e: Exception) {
                        // Log error but don't fail the operation
                        Log.e(TAG, "Error scheduling/cancelling reminder for task $taskId", e)
                    }
                }
            }
        }
    }

    override suspend fun getActiveListCount(): Int =
        withContext(dispatcher) {
            mutex.withLock {
                dao.getActiveListCount()
            }
        }

    /**
     * Helper function to update a task with a transformation.
     */
    private suspend fun updateTask(
        listId: String,
        taskId: String,
        transform: (TaskEntity) -> TaskEntity
    ) {
        withContext(dispatcher) {
            mutex.withLock {
                val task = dao.getTaskById(taskId) ?: return@withLock
                val updatedTask = transform(task)
                dao.updateTask(updatedTask)
            }
        }
    }
}
