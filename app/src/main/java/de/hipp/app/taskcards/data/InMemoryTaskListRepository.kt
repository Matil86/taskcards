package de.hipp.app.taskcards.data

import android.util.Log
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.TaskItem
import de.hipp.app.taskcards.worker.ReminderScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Simple in-memory repository to satisfy app behavior and tests.
 * Replace with a Firestore-backed implementation later using the same interface.
 *
 * @param dispatcher The dispatcher to use for background operations. Defaults to Dispatchers.Default.
 *                   In tests, pass the test dispatcher to ensure proper control over coroutine execution.
 * @param reminderScheduler Optional scheduler for task reminder notifications. Null in tests.
 * @param preferencesRepo Optional repository for user preferences. Null in tests.
 */
class InMemoryTaskListRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val reminderScheduler: ReminderScheduler? = null,
    private val preferencesRepo: PreferencesRepository? = null
) : TaskListRepository {
    private val lists = MutableStateFlow<Map<String, List<TaskItem>>>(emptyMap())
    private val mutex = Mutex()

    companion object {
        private const val TAG = "InMemoryTaskListRepo"
    }

    override fun observeTasks(listId: String): Flow<List<TaskItem>> =
        lists.map { it[listId].orEmpty().sortedWith(compareBy<TaskItem> { it.removed }.thenBy { it.order }) }

    override suspend fun addTask(listId: String, text: String): TaskItem =
        withContext(dispatcher) {
            mutex.withLock {
                val trimmedText = text.trim()
                require(trimmedText.isNotEmpty()) { "Task text cannot be empty" }
                require(trimmedText.length <= TaskListRepository.MAX_TASK_TEXT_LENGTH) {
                    "Task text cannot exceed ${TaskListRepository.MAX_TASK_TEXT_LENGTH} characters"
                }

                val current = lists.value[listId].orEmpty()
                val newOrder = (current.filter { !it.removed }.minOfOrNull { it.order } ?: 0) - 1
                val item = TaskItem(listId = listId, text = trimmedText, order = newOrder)
                lists.value = lists.value.toMutableMap().apply { put(listId, current + item) }
                item
            }
        }

    override suspend fun removeTask(listId: String, taskId: String) { updateItem(listId, taskId) { it.copy(removed = true) } }

    override suspend fun restoreTask(listId: String, taskId: String) { updateItem(listId, taskId) { it.copy(removed = false) } }

    override suspend fun markDone(listId: String, taskId: String, done: Boolean) { updateItem(listId, taskId) { it.copy(done = done) } }

    override suspend fun moveTask(listId: String, taskId: String, toIndex: Int) =
        withContext(dispatcher) {
            mutex.withLock {
                val current = lists.value[listId].orEmpty()
                val active = current.filter { !it.removed }.sortedBy { it.order }.toMutableList()
                val moving = active.firstOrNull { it.id == taskId } ?: return@withLock
                val fromIndex = active.indexOf(moving)
                if (fromIndex == -1) return@withLock
                val boundedIndex = toIndex.coerceIn(0, active.lastIndex)
                active.removeAt(fromIndex)
                active.add(boundedIndex, moving)
                // Reassign compact orders: 0,1,2 ... lower = higher priority so start at 0
                val reordered = active.mapIndexed { idx, it -> it.copy(order = idx) }
                // Merge with removed items unchanged
                val removed = current.filter { it.removed }
                val merged = (reordered + removed).sortedWith(compareBy<TaskItem> { it.removed }.thenBy { it.order })
                lists.value = lists.value.toMutableMap().apply { put(listId, merged) }
            }
        }

    override suspend fun clearList(listId: String) =
        withContext(dispatcher) {
            mutex.withLock {
                lists.value = lists.value.toMutableMap().apply { remove(listId) }
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

                // Update task in memory (inline to avoid mutex deadlock)
                val current = lists.value[listId].orEmpty()
                val updated = current.map {
                    if (it.id == taskId) it.copy(dueDate = dueDate, reminderType = finalReminderType)
                    else it
                }
                lists.value = lists.value.toMutableMap().apply { put(listId, updated) }

                // Get the updated task for reminder scheduling
                val task = updated.firstOrNull { it.id == taskId }

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
                        try {
                            Log.e(TAG, "Error scheduling/cancelling reminder for task $taskId", e)
                        } catch (_: Exception) {
                            // Ignore Log exceptions in unit tests
                        }
                    }
                }
            }
        }
    }

    override suspend fun getActiveListCount(): Int =
        withContext(dispatcher) {
            mutex.withLock {
                lists.value.size
            }
        }

    private suspend fun updateItem(listId: String, taskId: String, transform: (TaskItem) -> TaskItem) {
        withContext(dispatcher) {
            mutex.withLock {
                val current = lists.value[listId].orEmpty()
                val updated = current.map { if (it.id == taskId) transform(it) else it }
                lists.value = lists.value.toMutableMap().apply { put(listId, updated) }
            }
        }
    }
}
