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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    // Debounce job for skipCard — absorbs rapid successive swipes to prevent N+1 Firestore writes
    private var skipDebounceJob: Job? = null

    /**
     * Skips the current drawn card — puts it back at the bottom of the deck.
     * The task stays active (not done, not removed). It will reappear on next draw.
     *
     * Uses totalActive (full deck count) as the target index so that tasks beyond
     * the 5-card visible window are correctly moved to the true end of the deck.
     * Debounced by 300ms to prevent N+1 Firestore writes from rapid swipe input.
     */
    fun skipCard(taskId: String) {
        skipDebounceJob?.cancel()
        skipDebounceJob = viewModelScope.launch(dispatcher) {
            delay(300L)  // 300ms debounce — absorbs rapid swipes before writing to Firestore
            try {
                val activeCount = state.value.totalActive  // FIX: was visibleCards.size (capped at 5)
                repo.moveTask(listId, taskId, activeCount - 1)
                _errorState.value = null
                analytics.logEvent(
                    AnalyticsEvents.SWIPE_GESTURE_USED,
                    mapOf(AnalyticsParams.GESTURE_TYPE to "skip")
                )
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_update, e.message ?: "")
                Logger.e(TAG, { "Error skipping card" }, e)
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
