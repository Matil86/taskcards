package de.hipp.app.taskcards.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.analytics.Analytics
import de.hipp.app.taskcards.analytics.AnalyticsEvents
import de.hipp.app.taskcards.analytics.AnalyticsParams
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.di.StringProvider
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.SearchFilter
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
class ListViewModel(
    savedStateHandle: SavedStateHandle,
    private val repo: TaskListRepository,
    internal val prefsRepo: PreferencesRepository,
    internal val strings: StringProvider,
    internal val analytics: Analytics,
    internal val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    internal val listId: String = savedStateHandle.get<String>("listId") ?: ""

    init {
        viewModelScope.launch(dispatcher) {
            try {
                prefsRepo.setLastUsedListId(listId)
            } catch (e: Exception) {
                logError("Failed to save last used list ID", e)
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

    internal val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    internal val _searchFilter = MutableStateFlow(SearchFilter())
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
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    fun add(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(dispatcher) {
            try {
                repo.addTask(listId, text.trim())
                _errorState.value = null
                analytics.logEvent(AnalyticsEvents.TASK_CREATED, mapOf(AnalyticsParams.LIST_ID to listId))
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_add, e.message ?: "")
                logError("Error adding task", e)
            }
        }
    }

    fun remove(taskId: String) {
        viewModelScope.launch(dispatcher) {
            try {
                repo.removeTask(listId, taskId)
                _errorState.value = null
                analytics.logEvent(AnalyticsEvents.TASK_DELETED, mapOf(AnalyticsParams.LIST_ID to listId))
                analytics.logEvent(AnalyticsEvents.SWIPE_GESTURE_USED, mapOf(AnalyticsParams.GESTURE_TYPE to "remove"))
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_remove, e.message ?: "")
                logError("Error removing task", e)
            }
        }
    }

    fun restore(taskId: String) {
        viewModelScope.launch(dispatcher) {
            try {
                repo.restoreTask(listId, taskId)
                _errorState.value = null
                analytics.logEvent(AnalyticsEvents.TASK_RESTORED, mapOf(AnalyticsParams.LIST_ID to listId))
                analytics.logEvent(AnalyticsEvents.SWIPE_GESTURE_USED, mapOf(AnalyticsParams.GESTURE_TYPE to "restore"))
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_restore, e.message ?: "")
                logError("Error restoring task", e)
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
                logError("Error toggling done status", e)
            }
        }
    }

    fun move(taskId: String, toIndex: Int) {
        viewModelScope.launch(dispatcher) {
            try {
                repo.moveTask(listId, taskId, toIndex)
                _errorState.value = null
                analytics.logEvent(AnalyticsEvents.TASK_REORDERED, mapOf(AnalyticsParams.LIST_ID to listId, AnalyticsParams.TO_INDEX to toIndex))
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_move, e.message ?: "")
                logError("Error moving task", e)
            }
        }
    }

    fun clearError() { _errorState.value = null }

    fun generateListText(): String = generateListText(state.value.tasks)

    private fun logError(msg: String, e: Exception) = runCatching { Logger.e(TAG, { msg }, e) }

    companion object {
        private const val TAG = "ListViewModel"
    }
}
