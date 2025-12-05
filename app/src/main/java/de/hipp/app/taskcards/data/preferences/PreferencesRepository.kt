package de.hipp.app.taskcards.data.preferences

import de.hipp.app.taskcards.model.SavedSearch
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing app preferences and settings.
 * Provides reactive access to user preferences via Flow.
 */
interface PreferencesRepository {
    val highContrastMode: Flow<Boolean>
    val settings: Flow<Settings>
    val savedSearches: Flow<List<SavedSearch>>

    suspend fun setHighContrastMode(enabled: Boolean)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setReminderTime(hour: Int, minute: Int)
    suspend fun setNotificationSound(enabled: Boolean)
    suspend fun setNotificationVibration(enabled: Boolean)
    suspend fun setLanguage(languageCode: String)

    fun getSavedSearchesForList(listId: String): Flow<List<SavedSearch>>
    suspend fun saveSavedSearch(search: SavedSearch)
    suspend fun updateSavedSearch(search: SavedSearch)
    suspend fun deleteSavedSearch(searchId: String)

    suspend fun setLastUsedListId(listId: String)
    suspend fun getLastUsedListId(): String?
}
