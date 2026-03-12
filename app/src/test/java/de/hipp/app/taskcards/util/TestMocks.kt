package de.hipp.app.taskcards.util

import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.preferences.Settings
import de.hipp.app.taskcards.di.StringProvider
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.SearchFilter
import de.hipp.app.taskcards.model.StatusFilter
import de.hipp.app.taskcards.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Shared test exception for simulating repository errors in unit tests.
 */
internal class TestRepositoryException(message: String) : Exception(message)

/**
 * Shared mock [StringProvider] for unit tests.
 *
 * Returns a fixed "Error message" string for all resource IDs, making
 * test assertions deterministic regardless of actual string resources.
 */
internal class MockStringProvider : StringProvider {
    override fun getString(resId: Int): String = "Error message"
    override fun getString(resId: Int, vararg formatArgs: Any): String = "Error message"
}

/**
 * In-memory [PreferencesRepository] for unit tests.
 *
 * Stores saved searches in a [MutableStateFlow]; all other preference
 * mutations are no-ops so tests only need to set up the data they care about.
 */
internal class MockPreferencesRepository : PreferencesRepository {
    private val _savedSearches = MutableStateFlow<List<SavedSearch>>(emptyList())
    private val _settings = MutableStateFlow(Settings())

    override val highContrastMode: Flow<Boolean> = flowOf(false)
    override val settings: Flow<Settings> = _settings
    override val savedSearches: Flow<List<SavedSearch>> = _savedSearches

    override suspend fun setHighContrastMode(enabled: Boolean) {}
    override suspend fun setRemindersEnabled(enabled: Boolean) {}
    override suspend fun setReminderTime(hour: Int, minute: Int) {}
    override suspend fun setNotificationSound(enabled: Boolean) {}
    override suspend fun setNotificationVibration(enabled: Boolean) {}
    override suspend fun setLanguage(languageCode: String) {}

    override fun getSavedSearchesForList(listId: String): Flow<List<SavedSearch>> =
        flowOf(_savedSearches.value.filter { it.listId == listId })

    override suspend fun saveSavedSearch(search: SavedSearch) {
        _savedSearches.value = _savedSearches.value + search
    }

    override suspend fun updateSavedSearch(search: SavedSearch) {
        _savedSearches.value = _savedSearches.value.map {
            if (it.id == search.id) search else it
        }
    }

    override suspend fun deleteSavedSearch(searchId: String) {
        _savedSearches.value = _savedSearches.value.filter { it.id != searchId }
    }

    override suspend fun setLastUsedListId(listId: String) {}
    override suspend fun getLastUsedListId(): String? = null
}

/**
 * [TaskListRepository] that throws [TestRepositoryException] for every write
 * operation, used to verify that ViewModels correctly surface repository errors.
 */
internal class FailingTaskListRepository : TaskListRepository {
    override fun observeTasks(listId: String): Flow<List<TaskItem>> = flowOf(emptyList())

    override suspend fun addTask(listId: String, text: String): TaskItem =
        throw TestRepositoryException("Test exception")

    override suspend fun removeTask(listId: String, taskId: String) =
        throw TestRepositoryException("Test exception")

    override suspend fun restoreTask(listId: String, taskId: String) =
        throw TestRepositoryException("Test exception")

    override suspend fun markDone(listId: String, taskId: String, done: Boolean) =
        throw TestRepositoryException("Test exception")

    override suspend fun moveTask(listId: String, taskId: String, toIndex: Int) =
        throw TestRepositoryException("Test exception")

    override suspend fun clearList(listId: String) =
        throw TestRepositoryException("Test exception")

    override suspend fun updateTaskDueDate(
        listId: String,
        taskId: String,
        dueDate: Long?,
        reminderType: ReminderType
    ) = throw TestRepositoryException("Test exception")

    override suspend fun getActiveListCount(): Int = 0
}
