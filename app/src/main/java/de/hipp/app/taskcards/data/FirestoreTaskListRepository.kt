package de.hipp.app.taskcards.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.TaskItem
import de.hipp.app.taskcards.worker.ReminderScheduler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Firestore-backed implementation of TaskListRepository.
 * Provides cloud-synchronized storage across devices.
 * Lists are shared and collaborative - any authenticated user can access any list.
 *
 * Data structure:
 * /lists/{listId}/tasks/{taskId}
 *   - text: String
 *   - order: Int
 *   - done: Boolean
 *   - removed: Boolean
 *   - dueDate: Long? (timestamp in millis)
 *   - reminderType: String (ReminderType enum name)
 *   - timestamp: Long (server timestamp for sync)
 *
 * @param firestore The Firestore instance
 * @param reminderScheduler Optional scheduler for task reminder notifications. Null in tests.
 * @param preferencesRepo Optional repository for user preferences. Null in tests.
 */
class FirestoreTaskListRepository(
    private val firestore: FirebaseFirestore,
    private val reminderScheduler: ReminderScheduler? = null,
    private val preferencesRepo: PreferencesRepository? = null
) : TaskListRepository {
    private val mutex = Mutex()

    companion object {
        private const val TAG = "FirestoreTaskRepo"
        private const val COLLECTION_LISTS = "lists"
        private const val COLLECTION_TASKS = "tasks"
    }

    /**
     * Get reference to tasks collection for a specific list.
     */
    private fun getTasksCollection(listId: String) =
        firestore.collection(COLLECTION_LISTS)
            .document(listId)
            .collection(COLLECTION_TASKS)

    override fun observeTasks(listId: String): Flow<List<TaskItem>> = callbackFlow {
        val listener = getTasksCollection(listId)
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing tasks for list $listId", error)
                    // Emit empty list instead of crashing - graceful error handling
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val tasks = snapshot.documents.mapNotNull { doc ->
                        parseTaskDocument(doc, listId)
                    }
                    trySend(tasks)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun addTask(listId: String, text: String): TaskItem =
        mutex.withLock {
            val trimmedText = text.trim()
            require(trimmedText.isNotEmpty()) { "Task text cannot be empty" }
            require(trimmedText.length <= TaskListRepository.MAX_TASK_TEXT_LENGTH) {
                "Task text cannot exceed ${TaskListRepository.MAX_TASK_TEXT_LENGTH} characters"
            }

            try {
                // Get minimum order value for new task positioning
                val snapshot = getTasksCollection(listId)
                    .whereEqualTo("removed", false)
                    .orderBy("order", Query.Direction.ASCENDING)
                    .limit(1)
                    .get()
                    .await()

                val minOrder = snapshot.documents.firstOrNull()?.getLong("order")?.toInt() ?: 0
                val newOrder = minOrder - 1

                // Create new task document with auto-generated ID
                val docRef = getTasksCollection(listId).document()
                val task = TaskItem(
                    id = docRef.id,
                    listId = listId,
                    text = trimmedText,
                    order = newOrder,
                    done = false,
                    removed = false
                )

                val taskData = hashMapOf(
                    "text" to task.text,
                    "order" to task.order,
                    "done" to task.done,
                    "removed" to task.removed,
                    "dueDate" to task.dueDate,
                    "reminderType" to task.reminderType.name,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                docRef.set(taskData).await()
                Log.d(TAG, "Added task ${task.id} to list $listId")

                task
            } catch (e: Exception) {
                Log.e(TAG, "Error adding task to list $listId", e)
                throw e
            }
        }

    override suspend fun removeTask(listId: String, taskId: String) {
        updateTaskField(listId, taskId, "removed", true)
    }

    override suspend fun restoreTask(listId: String, taskId: String) {
        updateTaskField(listId, taskId, "removed", false)
    }

    override suspend fun markDone(listId: String, taskId: String, done: Boolean) {
        updateTaskField(listId, taskId, "done", done)
    }

    override suspend fun moveTask(listId: String, taskId: String, toIndex: Int) =
        mutex.withLock {
            try {
                // Fetch all non-removed tasks ordered by current order
                val snapshot = getTasksCollection(listId)
                    .whereEqualTo("removed", false)
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val tasks = snapshot.documents.map { doc ->
                    doc.id to (doc.getLong("order")?.toInt() ?: 0)
                }.toMutableList()

                // Find the moving task
                val movingIndex = tasks.indexOfFirst { it.first == taskId }
                if (movingIndex == -1) {
                    Log.w(TAG, "Task $taskId not found in list $listId")
                    return@withLock
                }

                val movingTask = tasks.removeAt(movingIndex)
                val boundedIndex = toIndex.coerceIn(0, tasks.size)
                tasks.add(boundedIndex, movingTask)

                // Batch update all tasks with new compact orders
                val batch = firestore.batch()
                tasks.forEachIndexed { idx, (docId, _) ->
                    val docRef = getTasksCollection(listId).document(docId)
                    batch.update(docRef, "order", idx)
                }

                batch.commit().await()
                Log.d(TAG, "Moved task $taskId to index $toIndex in list $listId")
            } catch (e: Exception) {
                Log.e(TAG, "Error moving task $taskId in list $listId", e)
                throw e
            }
        }

    override suspend fun clearList(listId: String) {
        mutex.withLock {
            try {
                val snapshot = getTasksCollection(listId).get().await()
                val batch = firestore.batch()

                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.commit().await()
                Log.d(TAG, "Cleared all tasks from list $listId")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing list $listId", e)
                throw e
            }
        }
    }

    override suspend fun getActiveListCount(): Int {
        return try {
            // Count all lists in the top-level collection
            val snapshot = firestore.collection(COLLECTION_LISTS)
                .get()
                .await()

            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active list count", e)
            0
        }
    }

    override suspend fun updateTaskDueDate(
        listId: String,
        taskId: String,
        dueDate: Long?,
        reminderType: ReminderType
    ) {
        mutex.withLock {
            try {
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

                val updates = hashMapOf<String, Any?>(
                    "dueDate" to dueDate,
                    "reminderType" to finalReminderType.name
                )

                getTasksCollection(listId)
                    .document(taskId)
                    .update(updates)
                    .await()

                Log.d(TAG, "Updated task $taskId due date and reminder type")

                // Get the updated task for reminder scheduling
                val taskDoc = getTasksCollection(listId).document(taskId).get().await()
                val task = parseTaskDocument(taskDoc, listId)

                // Schedule or cancel reminder if scheduler is available
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
            } catch (e: Exception) {
                Log.e(TAG, "Error updating due date for task $taskId in list $listId", e)
                throw e
            }
        }
    }

    /**
     * Helper to parse a Firestore document into a TaskItem.
     * Returns null if parsing fails.
     */
    private fun parseTaskDocument(doc: com.google.firebase.firestore.DocumentSnapshot, listId: String): TaskItem? {
        return try {
            TaskItem(
                id = doc.id,
                listId = listId,
                text = doc.getString("text") ?: "",
                order = doc.getLong("order")?.toInt() ?: 0,
                done = doc.getBoolean("done") ?: false,
                removed = doc.getBoolean("removed") ?: false,
                dueDate = doc.getLong("dueDate"),
                reminderType = try {
                    ReminderType.valueOf(doc.getString("reminderType") ?: "NONE")
                } catch (e: IllegalArgumentException) {
                    ReminderType.NONE
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing task document ${doc.id}", e)
            null
        }
    }

    /**
     * Helper to update a single field in a task document.
     */
    private suspend fun updateTaskField(listId: String, taskId: String, field: String, value: Any) {
        try {
            getTasksCollection(listId)
                .document(taskId)
                .update(field, value)
                .await()
            Log.d(TAG, "Updated task $taskId field '$field' to $value")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task $taskId in list $listId", e)
            throw e
        }
    }
}
