package de.hipp.app.taskcards.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.hipp.app.taskcards.model.SavedSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore-based implementation of PreferencesRepository.
 * Stores preferences persistently using Android DataStore.
 */
class PreferencesRepositoryImpl(private val context: Context) : PreferencesRepository {

    // SharedPreferences cache for synchronous access (needed in attachBaseContext)
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private object PreferencesKeys {
        val HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val NOTIFICATION_SOUND = booleanPreferencesKey("notification_sound")
        val NOTIFICATION_VIBRATION = booleanPreferencesKey("notification_vibration")
        val SAVED_SEARCHES = stringPreferencesKey("saved_searches")
        val LAST_USED_LIST_ID = stringPreferencesKey("last_used_list_id")
        val LANGUAGE = stringPreferencesKey("language")
    }

    companion object {
        private const val MAX_SAVED_SEARCHES_PER_LIST = 10
    }

    override val highContrastMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HIGH_CONTRAST_MODE] ?: false
        }

    override val settings: Flow<Settings> = context.dataStore.data
        .map { preferences ->
            Settings(
                remindersEnabled = preferences[PreferencesKeys.REMINDERS_ENABLED] ?: true,
                reminderHour = preferences[PreferencesKeys.REMINDER_HOUR] ?: 9,
                reminderMinute = preferences[PreferencesKeys.REMINDER_MINUTE] ?: 0,
                notificationSound = preferences[PreferencesKeys.NOTIFICATION_SOUND] ?: true,
                notificationVibration = preferences[PreferencesKeys.NOTIFICATION_VIBRATION] ?: true,
                highContrastMode = preferences[PreferencesKeys.HIGH_CONTRAST_MODE] ?: false,
                language = preferences[PreferencesKeys.LANGUAGE] ?: "system"
            )
        }

    override suspend fun setHighContrastMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_CONTRAST_MODE] = enabled
        }
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDERS_ENABLED] = enabled
        }
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_HOUR] = hour
            preferences[PreferencesKeys.REMINDER_MINUTE] = minute
        }
    }

    override suspend fun setNotificationSound(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_SOUND] = enabled
        }
    }

    override suspend fun setNotificationVibration(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_VIBRATION] = enabled
        }
    }

    override suspend fun setLanguage(languageCode: String) {
        require(languageCode in listOf("en", "de", "ja", "system")) {
            "Language code must be one of: en, de, ja, system"
        }
        // Write to both DataStore and SharedPreferences for synchronous access
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = languageCode
        }
        prefs.edit().putString("language", languageCode).apply()
    }

    /**
     * Get language code synchronously from SharedPreferences cache.
     * Used in attachBaseContext() where suspend functions cannot be called.
     * Falls back to "system" if not set.
     */
    fun getLanguageSync(): String {
        return prefs.getString("language", "system") ?: "system"
    }

    /**
     * Observes all saved searches across all lists.
     * Parses the serialized string from DataStore into SavedSearch objects.
     */
    override val savedSearches: Flow<List<SavedSearch>> = context.dataStore.data
        .map { preferences ->
            val serialized = preferences[PreferencesKeys.SAVED_SEARCHES] ?: ""
            SavedSearchSerializer.deserialize(serialized)
        }

    /**
     * Gets saved searches for a specific list.
     */
    override fun getSavedSearchesForList(listId: String): Flow<List<SavedSearch>> {
        return savedSearches.map { searches ->
            searches.filter { it.listId == listId }
        }
    }

    /**
     * Saves a new search. If the list already has 10 saved searches, throws an exception.
     * @throws IllegalStateException if list already has max saved searches
     */
    override suspend fun saveSavedSearch(search: SavedSearch) {
        context.dataStore.edit { preferences ->
            val current = SavedSearchSerializer.deserialize(
                preferences[PreferencesKeys.SAVED_SEARCHES] ?: ""
            )

            // Check limit per list
            val listSearchCount = current.count { it.listId == search.listId }
            require(listSearchCount < MAX_SAVED_SEARCHES_PER_LIST) {
                "Maximum $MAX_SAVED_SEARCHES_PER_LIST saved searches per list"
            }

            val updated = current + search
            preferences[PreferencesKeys.SAVED_SEARCHES] = SavedSearchSerializer.serialize(updated)
        }
    }

    /**
     * Updates an existing saved search.
     * @throws NoSuchElementException if search with given id doesn't exist
     */
    override suspend fun updateSavedSearch(search: SavedSearch) {
        context.dataStore.edit { preferences ->
            val current = SavedSearchSerializer.deserialize(
                preferences[PreferencesKeys.SAVED_SEARCHES] ?: ""
            )

            require(current.any { it.id == search.id }) {
                "Saved search with id ${search.id} not found"
            }

            val updated = current.map { if (it.id == search.id) search else it }
            preferences[PreferencesKeys.SAVED_SEARCHES] = SavedSearchSerializer.serialize(updated)
        }
    }

    /**
     * Deletes a saved search by id.
     */
    override suspend fun deleteSavedSearch(searchId: String) {
        context.dataStore.edit { preferences ->
            val current = SavedSearchSerializer.deserialize(
                preferences[PreferencesKeys.SAVED_SEARCHES] ?: ""
            )
            val updated = current.filter { it.id != searchId }
            preferences[PreferencesKeys.SAVED_SEARCHES] = SavedSearchSerializer.serialize(updated)
        }
    }

    /**
     * Sets the last used list ID.
     * This is used for smart navigation on app startup.
     */
    override suspend fun setLastUsedListId(listId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_USED_LIST_ID] = listId
        }
    }

    /**
     * Gets the last used list ID.
     * Returns null if no list has been used yet.
     */
    override suspend fun getLastUsedListId(): String? {
        return context.dataStore.data
            .map { it[PreferencesKeys.LAST_USED_LIST_ID] }
            .first()
    }
}
