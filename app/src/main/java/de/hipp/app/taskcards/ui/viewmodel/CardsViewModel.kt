package de.hipp.app.taskcards.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.analytics.Analytics
import de.hipp.app.taskcards.analytics.AnalyticsEvents
import de.hipp.app.taskcards.analytics.AnalyticsParams
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.di.StringProvider
import de.hipp.app.taskcards.model.TaskItem
import de.hipp.app.taskcards.ui.viewmodel.list.generateListText
import de.hipp.app.taskcards.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class CardsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repo: TaskListRepository,
    private val strings: StringProvider,
    private val analytics: Analytics,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val listId: String = savedStateHandle.get<String>("listId") ?: ""

    // Secondary constructor for backward compatibility (tests and manual factory creation)
    constructor(
        listId: String,
        repo: TaskListRepository,
        strings: StringProvider,
        dispatcher: CoroutineDispatcher = Dispatchers.Main
    ) : this(
        savedStateHandle = SavedStateHandle(mapOf("listId" to listId)),
        repo = repo,
        strings = strings,
        analytics = object : de.hipp.app.taskcards.analytics.Analytics {
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
        val visibleCards: List<TaskItem> = emptyList(),
        val totalActive: Int = 0,
        val error: String? = null
    )

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val state: StateFlow<UiState> = repo.observeTasks(listId)
        .map { items ->
            Logger.d(TAG) { "=== CardsViewModel State Update ===" }
            Logger.d(TAG) { "ListId: $listId" }
            Logger.d(TAG) { "Total items from repo: ${items.size}" }
            Logger.d(TAG) { "Items breakdown:" }
            items.forEachIndexed { idx, item ->
                val itemInfo = "id=${item.id.take(8)}, text='${item.text}', " +
                    "removed=${item.removed}, done=${item.done}, order=${item.order}"
                Logger.d(TAG) { "  [$idx] $itemInfo" }
            }

            val active = items.filter { !it.removed && !it.done }.sortedBy { it.order }

            Logger.d(TAG) { "Active tasks count: ${active.size}" }
            if (active.isEmpty()) {
                Logger.w(TAG) { "⚠️ NO ACTIVE TASKS - Cards will not be visible!" }
                Logger.w(TAG) { "⚠️ User should add tasks via List Screen" }
            }

            UiState(
                visibleCards = active.take(if (active.size < 5) active.size else 5),
                totalActive = active.size
            )
        }
        .stateIn(
            scope = viewModelScope,
            // WhileSubscribed(5000): Keeps flow active for 5s after last subscriber unsubscribes
            // This prevents rapid resubscription overhead during configuration changes
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState()
        )

    /**
     * Marks a task as done (or un-done) after the user swipes the card.
     * Logs analytics events for completed tasks.
     *
     * @param taskId The ID of the task to update.
     * @param complete `true` to mark the task done, `false` to undo.
     */
    fun swipeComplete(taskId: String, complete: Boolean) {
        viewModelScope.launch(dispatcher) {
            try {
                repo.markDone(listId, taskId, complete)
                _errorState.value = null

                // Log analytics event
                if (complete) {
                    analytics.logEvent(
                        AnalyticsEvents.TASK_COMPLETED,
                        mapOf(AnalyticsParams.LIST_ID to listId)
                    )
                    analytics.logEvent(
                        AnalyticsEvents.SWIPE_GESTURE_USED,
                        mapOf(AnalyticsParams.GESTURE_TYPE to "complete")
                    )
                }
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_update, e.message ?: "")
                Logger.e(TAG, { "Error marking task as done" }, e)
            }
        }
    }

    /** Clears the current error message so the snackbar is dismissed. */
    fun clearError() {
        _errorState.value = null
    }

    /**
     * Generate a text representation of the list for clipboard copy.
     * Includes visible cards (up to 5 active tasks).
     * @return Formatted text string with checkboxes for done/active tasks
     */
    fun generateListText(): String {
        return generateListText(state.value.visibleCards)
    }

    companion object {
        private const val TAG = "CardsViewModel"
    }
}
