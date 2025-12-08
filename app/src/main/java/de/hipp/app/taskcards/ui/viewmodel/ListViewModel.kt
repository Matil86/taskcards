package de.hipp.app.taskcards.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.analytics.Analytics
import de.hipp.app.taskcards.analytics.AnalyticsEvents
import de.hipp.app.taskcards.analytics.AnalyticsParams
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.di.StringProvider
import de.hipp.app.taskcards.model.DueDateRange
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.SearchFilter
import de.hipp.app.taskcards.model.StatusFilter
import de.hipp.app.taskcards.model.TaskItem
import de.hipp.app.taskcards.ui.viewmodel.list.applyFilters
import de.hipp.app.taskcards.ui.viewmodel.list.generateListText
import de.hipp.app.taskcards.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: TaskListRepository,
    private val prefsRepo: PreferencesRepository,
    private val strings: StringProvider,
    private val analytics: Analytics,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val listId: String = savedStateHandle.get<String>("listId") ?: ""

    init {
        // Save this list as the last used list for smart navigation on app startup
        viewModelScope.launch(dispatcher) {
            try {
                prefsRepo.setLastUsedListId(listId)
            } catch (e: Exception) {
                // Log but don't fail - this is non-critical
                try {
                    Logger.e(TAG, { "Failed to save last used list ID" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    // Secondary constructor for backward compatibility (tests and manual factory creation)
    constructor(
        listId: String,
        repo: TaskListRepository,
        prefsRepo: PreferencesRepository,
        strings: StringProvider,
        dispatcher: CoroutineDispatcher = Dispatchers.Main
    ) : this(
        savedStateHandle = SavedStateHandle(mapOf("listId" to listId)),
        repo = repo,
        prefsRepo = prefsRepo,
        strings = strings,
        analytics = object : Analytics {
            override fun logEvent(eventName: String, params: Map<String, Any>) {
                // No-op for testing
            }
            override fun setUserProperty(name: String, value: String) {
                // No-op for testing
            }
            override fun setCurrentScreen(screenName: String) {
                // No-op for testing
            }
        },
        dispatcher = dispatcher
    )

    data class UiState(
        val tasks: List<TaskItem> = emptyList(),
        val savedSearches: List<SavedSearch> = emptyList(),
        val searchFilter: SearchFilter = SearchFilter(),
        val error: String? = null
    )

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilter())
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    val state: StateFlow<UiState> = combine(
        repo.observeTasks(listId),
        prefsRepo.getSavedSearchesForList(listId),
        _searchFilter
    ) { tasks, savedSearches, filter ->
        UiState(
            tasks = applyFilters(tasks, filter),
            savedSearches = savedSearches,
            searchFilter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        // WhileSubscribed(5000): Keeps flow active for 5s after last subscriber unsubscribes
        // This prevents rapid resubscription overhead during configuration changes
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    fun add(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(dispatcher) {
            try {
                repo.addTask(listId, text.trim())
                _errorState.value = null

                // Log analytics event
                analytics.logEvent(
                    AnalyticsEvents.TASK_CREATED,
                    mapOf(AnalyticsParams.LIST_ID to listId)
                )
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_add, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error adding task" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    fun remove(taskId: String) {
        viewModelScope.launch(dispatcher) {
            try {
                repo.removeTask(listId, taskId)
                _errorState.value = null

                // Log analytics event
                analytics.logEvent(
                    AnalyticsEvents.TASK_DELETED,
                    mapOf(AnalyticsParams.LIST_ID to listId)
                )
                analytics.logEvent(
                    AnalyticsEvents.SWIPE_GESTURE_USED,
                    mapOf(AnalyticsParams.GESTURE_TYPE to "remove")
                )
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_remove, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error removing task" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    fun restore(taskId: String) {
        viewModelScope.launch(dispatcher) {
            try {
                repo.restoreTask(listId, taskId)
                _errorState.value = null

                // Log analytics event
                analytics.logEvent(
                    AnalyticsEvents.TASK_RESTORED,
                    mapOf(AnalyticsParams.LIST_ID to listId)
                )
                analytics.logEvent(
                    AnalyticsEvents.SWIPE_GESTURE_USED,
                    mapOf(AnalyticsParams.GESTURE_TYPE to "restore")
                )
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_restore, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error restoring task" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    fun toggleDone(taskId: String, done: Boolean) {
        viewModelScope.launch(dispatcher) {
            try {
                repo.markDone(listId, taskId, done)
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_update, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error toggling done status" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    fun move(taskId: String, toIndex: Int) {
        viewModelScope.launch(dispatcher) {
            try {
                repo.moveTask(listId, taskId, toIndex)
                _errorState.value = null

                // Log analytics event
                analytics.logEvent(
                    AnalyticsEvents.TASK_REORDERED,
                    mapOf(
                        AnalyticsParams.LIST_ID to listId,
                        AnalyticsParams.TO_INDEX to toIndex
                    )
                )
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_move, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error moving task" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    // Search and Filter Methods

    /**
     * Updates the search query. Debouncing is handled at the UI layer.
     */
    fun setSearchQuery(query: String) {
        _searchFilter.value = _searchFilter.value.copy(textQuery = query)
        analytics.logEvent(
            AnalyticsEvents.SEARCH_USED,
            mapOf(AnalyticsParams.LIST_ID to listId, "query_length" to query.length)
        )
    }

    /**
     * Sets the status filter (all, active only, done only, removed only).
     */
    fun setStatusFilter(filter: StatusFilter) {
        _searchFilter.value = _searchFilter.value.copy(statusFilter = filter)
        analytics.logEvent(
            AnalyticsEvents.FILTER_APPLIED,
            mapOf(AnalyticsParams.LIST_ID to listId, "filter_type" to "status")
        )
    }

    /**
     * Sets the due date range filter.
     */
    fun setDueDateRange(range: DueDateRange?) {
        _searchFilter.value = _searchFilter.value.copy(dueDateRange = range)
        if (range != null) {
            analytics.logEvent(
                AnalyticsEvents.FILTER_APPLIED,
                mapOf(AnalyticsParams.LIST_ID to listId, "filter_type" to "due_date")
            )
        }
    }

    /**
     * Clears all active filters.
     */
    fun clearAllFilters() {
        _searchFilter.value = SearchFilter()
        analytics.logEvent(
            AnalyticsEvents.FILTER_CLEARED,
            mapOf(AnalyticsParams.LIST_ID to listId)
        )
    }

    /**
     * Saves the current search/filter configuration.
     */
    fun saveCurrentSearch(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(dispatcher) {
            try {
                val search = SavedSearch(
                    name = name.trim(),
                    filter = _searchFilter.value,
                    listId = listId
                )
                prefsRepo.saveSavedSearch(search)
                _errorState.value = null
                analytics.logEvent(
                    AnalyticsEvents.SAVED_SEARCH_CREATED,
                    mapOf(AnalyticsParams.LIST_ID to listId)
                )
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_save_search, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error saving search" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    /**
     * Applies a saved search configuration.
     */
    fun applySavedSearch(savedSearch: SavedSearch) {
        _searchFilter.value = savedSearch.filter
        analytics.logEvent(
            AnalyticsEvents.SAVED_SEARCH_APPLIED,
            mapOf(AnalyticsParams.LIST_ID to listId, "search_id" to savedSearch.id)
        )
    }

    /**
     * Deletes a saved search.
     */
    fun deleteSavedSearch(searchId: String) {
        viewModelScope.launch(dispatcher) {
            try {
                prefsRepo.deleteSavedSearch(searchId)
                _errorState.value = null
                analytics.logEvent(
                    AnalyticsEvents.SAVED_SEARCH_DELETED,
                    mapOf(AnalyticsParams.LIST_ID to listId)
                )
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_delete_search, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error deleting search" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }


    /**
     * Generate a text representation of the list for clipboard copy.
     * Includes all active (non-removed) tasks.
     * @return Formatted text string with checkboxes for done/active tasks
     */
    fun generateListText(): String {
        return generateListText(state.value.tasks)
    }

    companion object {
        private const val TAG = "ListViewModel"
    }
}
