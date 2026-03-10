package de.hipp.app.taskcards.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.TaskListMetadataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
/**
 * ViewModel for smart navigation on app startup.
 * Determines the appropriate starting destination based on:
 * 1. Last used list (if exists)
 * 2. Single available list (if only one)
 * 3. List selector (if multiple lists)
 */
class StartupViewModel(
    private val metadataRepo: TaskListMetadataRepository,
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    sealed class StartupDestination {
        data class OpenList(val listId: String) : StartupDestination()
        object ShowListSelector : StartupDestination()
        object Loading : StartupDestination()
    }

    private val _destination = MutableStateFlow<StartupDestination>(StartupDestination.Loading)
    val destination: StateFlow<StartupDestination> = _destination.asStateFlow()

    init {
        determineStartupDestination()
    }

    private fun determineStartupDestination() {
        viewModelScope.launch {
            try {
                // Get all available lists
                val availableLists = metadataRepo.observeTaskLists().first()

                if (availableLists.isEmpty()) {
                    // No lists exist - show list selector to create first list
                    _destination.value = StartupDestination.ShowListSelector
                    Log.d(TAG, "No lists found, showing list selector")
                    return@launch
                }

                // Get last used list ID
                val lastUsedListId = prefsRepo.getLastUsedListId()

                // Check if last used list still exists
                val lastUsedListExists = lastUsedListId != null &&
                    availableLists.any { it.id == lastUsedListId }

                when {
                    // Last used list exists - use it
                    lastUsedListExists -> {
                        _destination.value = StartupDestination.OpenList(lastUsedListId!!)
                        Log.d(TAG, "Opening last used list: $lastUsedListId")
                    }
                    // Only one list exists - use it
                    availableLists.size == 1 -> {
                        val singleListId = availableLists.first().id
                        _destination.value = StartupDestination.OpenList(singleListId)
                        Log.d(TAG, "Opening single list: $singleListId")
                    }
                    // Multiple lists exist - show selector
                    else -> {
                        _destination.value = StartupDestination.ShowListSelector
                        Log.d(TAG, "Multiple lists found, showing list selector")
                    }
                }
            } catch (e: Exception) {
                // On error, default to list selector
                Log.e(TAG, "Error determining startup destination", e)
                _destination.value = StartupDestination.ShowListSelector
            }
        }
    }

    companion object {
        private const val TAG = "StartupViewModel"
    }
}
