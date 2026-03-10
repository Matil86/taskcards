package de.hipp.app.taskcards.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.hipp.app.taskcards.data.TaskListMetadataRepository
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.model.TaskList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the List Selector screen.
 * Manages task list metadata (create, rename, delete) and task counts.
 */
class ListSelectorViewModel(
    private val metadataRepo: TaskListMetadataRepository,
    private val taskRepo: TaskListRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ListSelectorViewModel"
    }

    data class ListWithCount(
        val list: TaskList,
        val taskCount: Int
    )

    data class UiState(
        val lists: List<ListWithCount> = emptyList(),
        val isLoading: Boolean = false
    )

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    /**
     * Observe all lists with their task counts.
     * For simplicity, task counts are set to 0 initially.
     * The UI can call getTaskCount() to get the actual count if needed.
     */
    val state: StateFlow<UiState> = metadataRepo.observeTaskLists()
        .map { lists ->
            try {
                Log.d(TAG, "Observing ${lists.size} lists")

                // Map lists to ListWithCount with 0 tasks initially
                // Actual counts can be retrieved separately if needed
                val listsWithCounts = lists.map { list ->
                    ListWithCount(list, 0)
                }

                UiState(lists = listsWithCounts)
            } catch (e: Exception) {
                Log.e(TAG, "Error observing lists", e)
                UiState()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState()
        )

    /**
     * Create a new task list and return the ID via callback once it has been written to the repository.
     */
    fun createList(name: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val listId = metadataRepo.createList(name)
                _errorState.value = null
                Log.d(TAG, "Created list: $listId")
                onSuccess(listId)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating list", e)
                _errorState.value = "Failed to create list: ${e.message}"
            }
        }
    }

    /**
     * Rename an existing task list.
     */
    fun renameList(listId: String, newName: String) {
        viewModelScope.launch {
            try {
                metadataRepo.renameList(listId, newName)
                _errorState.value = null
                Log.d(TAG, "Renamed list $listId to '$newName'")
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming list", e)
                _errorState.value = "Failed to rename list: ${e.message}"
            }
        }
    }

    /**
     * Delete a task list and all its tasks.
     */
    fun deleteList(listId: String) {
        viewModelScope.launch {
            try {
                // First clear all tasks from the list
                taskRepo.clearList(listId)

                // Then delete the list metadata
                metadataRepo.deleteList(listId)

                _errorState.value = null
                Log.d(TAG, "Deleted list $listId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting list", e)
                _errorState.value = "Failed to delete list: ${e.message}"
            }
        }
    }

    /**
     * Clear the current error state.
     */
    fun clearError() {
        _errorState.value = null
    }

    /**
     * Get task count for a specific list.
     * Returns via callback to avoid blocking.
     */
    fun getTaskCount(listId: String, callback: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                taskRepo.observeTasks(listId)
                    .stateIn(viewModelScope)
                    .value
                    .let { tasks ->
                        val count = tasks.count { !it.removed }
                        callback(count)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting task count for list $listId", e)
                callback(0)
            }
        }
    }
}
