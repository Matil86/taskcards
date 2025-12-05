package de.hipp.app.taskcards.data

import de.hipp.app.taskcards.model.TaskList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * In-memory implementation of TaskListMetadataRepository.
 * Stores list metadata in memory and provides thread-safe operations.
 * Initializes with one default list named "My Tasks".
 *
 * @param dispatcher The dispatcher to use for background operations. Defaults to Dispatchers.Default.
 *                   In tests, pass the test dispatcher to ensure proper control over coroutine execution.
 */
class InMemoryTaskListMetadataRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : TaskListMetadataRepository {

    private val lists = MutableStateFlow<Map<String, TaskList>>(emptyMap())
    private val mutex = Mutex()

    init {
        // Initialize with one default list
        val defaultListId = UUID.randomUUID().toString()
        val defaultList = TaskList(
            id = defaultListId,
            name = "My Tasks",
            createdAt = System.currentTimeMillis(),
            lastModifiedAt = System.currentTimeMillis()
        )
        lists.value = mapOf(defaultListId to defaultList)
    }

    override fun observeTaskLists(): Flow<List<TaskList>> =
        lists.map { it.values.sortedBy { list -> list.createdAt } }

    override suspend fun createList(name: String): String =
        withContext(dispatcher) {
            mutex.withLock {
                val trimmedName = name.trim()
                require(trimmedName.isNotEmpty()) { "List name cannot be empty" }
                require(trimmedName.length <= 100) { "List name cannot exceed 100 characters" }

                val listId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val newList = TaskList(
                    id = listId,
                    name = trimmedName,
                    createdAt = now,
                    lastModifiedAt = now
                )

                lists.value = lists.value.toMutableMap().apply {
                    put(listId, newList)
                }

                listId
            }
        }

    override suspend fun renameList(listId: String, newName: String): Unit =
        withContext(dispatcher) {
            mutex.withLock {
                val trimmedName = newName.trim()
                require(trimmedName.isNotEmpty()) { "List name cannot be empty" }
                require(trimmedName.length <= 100) { "List name cannot exceed 100 characters" }

                val existingList = lists.value[listId]
                    ?: throw IllegalArgumentException("List with ID $listId not found")

                val updatedList = existingList.copy(
                    name = trimmedName,
                    lastModifiedAt = System.currentTimeMillis()
                )

                lists.value = lists.value.toMutableMap().apply {
                    put(listId, updatedList)
                }
            }
        }

    override suspend fun deleteList(listId: String): Unit =
        withContext(dispatcher) {
            mutex.withLock {
                require(lists.value.containsKey(listId)) { "List with ID $listId not found" }

                lists.value = lists.value.toMutableMap().apply {
                    remove(listId)
                }
            }
        }

    override suspend fun getDefaultListId(): String? =
        withContext(dispatcher) {
            mutex.withLock {
                lists.value.values
                    .sortedBy { it.createdAt }
                    .firstOrNull()?.id
            }
        }
}
