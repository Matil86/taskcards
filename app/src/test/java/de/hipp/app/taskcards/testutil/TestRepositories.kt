package de.hipp.app.taskcards.testutil

import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.preferences.Settings
import de.hipp.app.taskcards.model.SavedSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Helper functions to create test repository instances.
 */
object TestRepositories {
    /**
     * Creates a fake PreferencesRepository for testing without requiring Android Context.
     * This fake implementation uses in-memory state flows instead of DataStore.
     */
    fun createPreferencesRepository(): PreferencesRepository {
        return FakePreferencesRepository()
    }
}

/**
 * Fake implementation of PreferencesRepository for testing.
 * Does not require Android Context or DataStore - uses in-memory state instead.
 */
class FakePreferencesRepository : PreferencesRepository {
    private val highContrastState = MutableStateFlow(false)
    private val settingsState = MutableStateFlow(Settings())
    private val savedSearchesState = MutableStateFlow<List<SavedSearch>>(emptyList())

    override val highContrastMode: Flow<Boolean> get() = highContrastState
    override val settings: Flow<Settings> get() = settingsState
    override val savedSearches: Flow<List<SavedSearch>> get() = savedSearchesState

    override suspend fun setHighContrastMode(enabled: Boolean) {
        highContrastState.value = enabled
        settingsState.value = settingsState.value.copy(highContrastMode = enabled)
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        settingsState.value = settingsState.value.copy(remindersEnabled = enabled)
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
        settingsState.value = settingsState.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    override suspend fun setNotificationSound(enabled: Boolean) {
        settingsState.value = settingsState.value.copy(notificationSound = enabled)
    }

    override suspend fun setNotificationVibration(enabled: Boolean) {
        settingsState.value = settingsState.value.copy(notificationVibration = enabled)
    }

    override suspend fun setLanguage(languageCode: String) {
        settingsState.value = settingsState.value.copy(language = languageCode)
    }

    override fun getSavedSearchesForList(listId: String): Flow<List<SavedSearch>> {
        return savedSearches.map { searches -> searches.filter { it.listId == listId } }
    }

    override suspend fun saveSavedSearch(search: SavedSearch) {
        val current = savedSearchesState.value
        val listSearchCount = current.count { it.listId == search.listId }
        require(listSearchCount < 10) { "Maximum 10 saved searches per list" }
        savedSearchesState.value = current + search
    }

    override suspend fun updateSavedSearch(search: SavedSearch) {
        val current = savedSearchesState.value
        require(current.any { it.id == search.id }) { "Saved search with id ${search.id} not found" }
        savedSearchesState.value = current.map { if (it.id == search.id) search else it }
    }

    override suspend fun deleteSavedSearch(searchId: String) {
        val current = savedSearchesState.value
        savedSearchesState.value = current.filter { it.id != searchId }
    }

    override suspend fun getLastUsedListId(): String? {
        return null // Not needed for tests
    }

    override suspend fun setLastUsedListId(listId: String) {
        // Not needed for tests
    }
}
