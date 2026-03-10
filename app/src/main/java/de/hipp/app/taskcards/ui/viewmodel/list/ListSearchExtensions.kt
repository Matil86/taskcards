package de.hipp.app.taskcards.ui.viewmodel.list

import androidx.lifecycle.viewModelScope
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.analytics.AnalyticsEvents
import de.hipp.app.taskcards.analytics.AnalyticsParams
import de.hipp.app.taskcards.model.DueDateRange
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.SearchFilter
import de.hipp.app.taskcards.model.StatusFilter
import de.hipp.app.taskcards.ui.viewmodel.ListViewModel
import de.hipp.app.taskcards.util.Logger
import kotlinx.coroutines.launch

/**
 * Extension functions for [ListViewModel] covering search and filter operations.
 *
 * Extracted from ListViewModel to keep that class focused on task-level orchestration
 * while giving filter/search logic a single, testable home.
 */

/**
 * Updates the search query. Debouncing is handled at the UI layer.
 */
fun ListViewModel.setSearchQuery(query: String) {
    _searchFilter.value = _searchFilter.value.copy(textQuery = query)
    analytics.logEvent(
        AnalyticsEvents.SEARCH_USED,
        mapOf(AnalyticsParams.LIST_ID to listId, "query_length" to query.length)
    )
}

/**
 * Sets the status filter (all, active only, done only, removed only).
 */
fun ListViewModel.setStatusFilter(filter: StatusFilter) {
    _searchFilter.value = _searchFilter.value.copy(statusFilter = filter)
    analytics.logEvent(
        AnalyticsEvents.FILTER_APPLIED,
        mapOf(AnalyticsParams.LIST_ID to listId, "filter_type" to "status")
    )
}

/**
 * Sets the due date range filter.
 */
fun ListViewModel.setDueDateRange(range: DueDateRange?) {
    _searchFilter.value = _searchFilter.value.copy(dueDateRange = range)
    if (range != null) {
        analytics.logEvent(
            AnalyticsEvents.FILTER_APPLIED,
            mapOf(AnalyticsParams.LIST_ID to listId, "filter_type" to "due_date")
        )
    }
}

/**
 * Clears all active filters and resets to the default [SearchFilter].
 */
fun ListViewModel.clearAllFilters() {
    _searchFilter.value = SearchFilter()
    analytics.logEvent(
        AnalyticsEvents.FILTER_CLEARED,
        mapOf(AnalyticsParams.LIST_ID to listId)
    )
}

/**
 * Saves the current search/filter configuration under the given [name].
 */
fun ListViewModel.saveCurrentSearch(name: String) {
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
                Logger.e("ListViewModel", { "Error saving search" }, e)
            } catch (_: Exception) {
                // Ignore Log exceptions in unit tests
            }
        }
    }
}

/**
 * Applies a previously saved search configuration as the active filter.
 */
fun ListViewModel.applySavedSearch(savedSearch: SavedSearch) {
    _searchFilter.value = savedSearch.filter
    analytics.logEvent(
        AnalyticsEvents.SAVED_SEARCH_APPLIED,
        mapOf(AnalyticsParams.LIST_ID to listId, "search_id" to savedSearch.id)
    )
}

/**
 * Deletes the saved search identified by [searchId].
 */
fun ListViewModel.deleteSavedSearch(searchId: String) {
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
                Logger.e("ListViewModel", { "Error deleting search" }, e)
            } catch (_: Exception) {
                // Ignore Log exceptions in unit tests
            }
        }
    }
}
