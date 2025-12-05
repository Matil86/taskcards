package de.hipp.app.taskcards.ui.viewmodel

import de.hipp.app.taskcards.data.InMemoryTaskListRepository
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.preferences.Settings
import de.hipp.app.taskcards.di.StringProvider
import de.hipp.app.taskcards.model.DueDateRange
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.StatusFilter
import de.hipp.app.taskcards.util.Constants
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ListViewModelTest : StringSpec({
    val testDispatcher = StandardTestDispatcher()
    lateinit var mockRepo: InMemoryTaskListRepository
    lateinit var mockPrefsRepo: MockPreferencesRepository
    lateinit var mockStrings: MockStringProvider
    lateinit var viewModel: ListViewModel

    // Helper to wait for state updates
    suspend fun waitForState(): ListViewModel.UiState {
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel.state.first()
    }

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        mockRepo = InMemoryTaskListRepository(testDispatcher)
        mockPrefsRepo = MockPreferencesRepository()
        mockStrings = MockStringProvider()
        viewModel = ListViewModel(
            listId = Constants.DEFAULT_LIST_ID,
            repo = mockRepo,
            prefsRepo = mockPrefsRepo,
            strings = mockStrings,
            dispatcher = testDispatcher
        )
    }

    afterTest {
        Dispatchers.resetMain()
    }

    // Basic Operations Tests

    "adding task updates state flow with new task" {
        runTest(testDispatcher) {
            // Start collecting the state to activate the flow
            backgroundScope.launch {
                viewModel.state.collect {}
            }

            viewModel.add("Test task")
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.state.value
            state.tasks.shouldHaveSize(1)
            state.tasks.first().text shouldBe "Test task"
            state.tasks.first().done shouldBe false
        }
    }

    "adding task trims whitespace" {
        runTest(testDispatcher) {
            viewModel.add("  Task with spaces  ")
            val state = waitForState()

            state.tasks.shouldHaveSize(1)
            state.tasks.first().text shouldBe "Task with spaces"
        }
    }

    "adding empty task is ignored" {
        runTest(testDispatcher) {
            viewModel.add("")
            val state = waitForState()

            state.tasks.shouldHaveSize(0)
        }
    }

    "adding whitespace-only task is ignored" {
        runTest(testDispatcher) {
            viewModel.add("   ")
            val state = waitForState()

            state.tasks.shouldHaveSize(0)
        }
    }

    "removing task marks task as removed" {
        runTest(testDispatcher) {
            viewModel.add("Task to remove")
            val state1 = waitForState()
            val taskId = state1.tasks.first().id

            viewModel.remove(taskId)
            val state2 = waitForState()

            // Default filter is ACTIVE_ONLY, so removed task should not appear
            state2.tasks.shouldHaveSize(0)
        }
    }

    "restoring task marks task as active" {
        runTest(testDispatcher) {
            viewModel.add("Task to restore")
            val state1 = waitForState()
            val taskId = state1.tasks.first().id

            viewModel.remove(taskId)
            waitForState()

            viewModel.restore(taskId)
            val state3 = waitForState()

            state3.tasks.shouldHaveSize(1)
            state3.tasks.first().removed shouldBe false
        }
    }

    "toggling done updates task done status" {
        runTest(testDispatcher) {
            viewModel.add("Task to complete")
            val state1 = waitForState()
            val taskId = state1.tasks.first().id

            viewModel.toggleDone(taskId, true)
            testDispatcher.scheduler.advanceUntilIdle()

            // ACTIVE_ONLY filter excludes done tasks, so switch to ALL
            viewModel.setStatusFilter(StatusFilter.ALL)
            val state2 = waitForState()

            state2.tasks.first().done shouldBe true
        }
    }

    "toggling done to false marks task as active" {
        runTest(testDispatcher) {
            viewModel.add("Task")
            val state1 = waitForState()
            val taskId = state1.tasks.first().id

            viewModel.toggleDone(taskId, true)
            waitForState()

            viewModel.toggleDone(taskId, false)
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(1)
            state2.tasks.first().done shouldBe false
        }
    }

    "moving task updates order correctly" {
        runTest(testDispatcher) {
            viewModel.add("Task A")
            viewModel.add("Task B")
            viewModel.add("Task C")
            val state1 = waitForState()

            val taskB = state1.tasks.firstOrNull { it.text == "Task B" }
            taskB.shouldNotBeNull()

            viewModel.move(taskB.id, 0)
            val state2 = waitForState()

            state2.tasks.map { it.text } shouldContainExactly listOf("Task B", "Task C", "Task A")
        }
    }

    // Search Functionality Tests

    "search query filters tasks by text content" {
        runTest(testDispatcher) {
            viewModel.add("Buy groceries")
            viewModel.add("Read book")
            viewModel.add("Buy tickets")
            waitForState()

            viewModel.setSearchQuery("buy")
            val state = waitForState()

            state.tasks.shouldHaveSize(2)
            state.tasks.map { it.text }.sorted() shouldContainExactly listOf("Buy groceries", "Buy tickets")
        }
    }

    "search query is case insensitive" {
        runTest(testDispatcher) {
            viewModel.add("UPPERCASE TASK")
            viewModel.add("lowercase task")
            viewModel.add("MiXeD CaSe TaSk")
            waitForState()

            viewModel.setSearchQuery("task")
            val state = waitForState()

            state.tasks.shouldHaveSize(3)
        }
    }

    "empty search query shows all tasks" {
        runTest(testDispatcher) {
            viewModel.add("Task 1")
            viewModel.add("Task 2")
            viewModel.add("Task 3")
            waitForState()

            viewModel.setSearchQuery("")
            val state = waitForState()

            state.tasks.shouldHaveSize(3)
        }
    }

    "search with no matches returns empty list" {
        runTest(testDispatcher) {
            viewModel.add("Buy groceries")
            viewModel.add("Read book")
            waitForState()

            viewModel.setSearchQuery("nonexistent")
            val state = waitForState()

            state.tasks.shouldHaveSize(0)
        }
    }

    "search handles special characters correctly" {
        runTest(testDispatcher) {
            viewModel.add("Task with @special #characters")
            viewModel.add("Normal task")
            waitForState()

            viewModel.setSearchQuery("@special")
            val state = waitForState()

            state.tasks.shouldHaveSize(1)
            state.tasks.first().text shouldBe "Task with @special #characters"
        }
    }

    "search handles unicode characters" {
        runTest(testDispatcher) {
            viewModel.add("Aufgabe mit Umlauten äöü")
            viewModel.add("日本語のタスク")
            viewModel.add("Regular task")
            waitForState()

            viewModel.setSearchQuery("äöü")
            val state = waitForState()

            state.tasks.shouldHaveSize(1)
            state.tasks.first().text shouldContain "Umlauten"
        }
    }

    // Status Filter Tests

    "status filter ALL shows all tasks" {
        runTest(testDispatcher) {
            viewModel.add("Active task")
            viewModel.add("Done task")
            viewModel.add("Removed task")
            val state1 = waitForState()

            val task2 = state1.tasks.firstOrNull { it.text == "Done task" }
            val task3 = state1.tasks.firstOrNull { it.text == "Removed task" }
            task2.shouldNotBeNull()
            task3.shouldNotBeNull()

            viewModel.toggleDone(task2.id, true)
            viewModel.remove(task3.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.setStatusFilter(StatusFilter.ALL)
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(3)
        }
    }

    "status filter ACTIVE_ONLY shows only active tasks" {
        runTest(testDispatcher) {
            viewModel.add("Active task")
            viewModel.add("Done task")
            viewModel.add("Removed task")
            val state1 = waitForState()

            val doneTask = state1.tasks.firstOrNull { it.text == "Done task" }
            val removedTask = state1.tasks.firstOrNull { it.text == "Removed task" }
            doneTask.shouldNotBeNull()
            removedTask.shouldNotBeNull()

            viewModel.toggleDone(doneTask.id, true)
            viewModel.remove(removedTask.id)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.setStatusFilter(StatusFilter.ACTIVE_ONLY)
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(1)
            state2.tasks.first().text shouldBe "Active task"
            state2.tasks.first().done shouldBe false
            state2.tasks.first().removed shouldBe false
        }
    }

    "status filter DONE_ONLY shows only completed tasks" {
        runTest(testDispatcher) {
            viewModel.add("Active task")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("Done task")
            val state1 = waitForState()

            val doneTask = state1.tasks.firstOrNull { it.text == "Done task" }
            doneTask.shouldNotBeNull()

            viewModel.toggleDone(doneTask.id, true)
            waitForState()

            viewModel.setStatusFilter(StatusFilter.DONE_ONLY)
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(1)
            state2.tasks.first().text shouldBe "Done task"
            state2.tasks.first().done shouldBe true
        }
    }

    "status filter REMOVED_ONLY shows only removed tasks" {
        runTest(testDispatcher) {
            viewModel.add("Active task")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("Removed task")
            val state1 = waitForState()

            val removedTask = state1.tasks.firstOrNull { it.text == "Removed task" }
            removedTask.shouldNotBeNull()

            viewModel.remove(removedTask.id)
            waitForState()

            viewModel.setStatusFilter(StatusFilter.REMOVED_ONLY)
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(1)
            state2.tasks.first().text shouldBe "Removed task"
            state2.tasks.first().removed shouldBe true
        }
    }

    // Due Date Filter Tests

    "due date filter today shows only tasks due today" {
        runTest(testDispatcher) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 12)
            }.timeInMillis

            // Add tasks
            viewModel.add("Due today")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("Due tomorrow")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("No due date")
            val state1 = waitForState()

            // Set due dates
            val todayTask = state1.tasks.firstOrNull { it.text == "Due today" }
            val tomorrowTask = state1.tasks.firstOrNull { it.text == "Due tomorrow" }
            todayTask.shouldNotBeNull()
            tomorrowTask.shouldNotBeNull()

            mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, todayTask.id, today, de.hipp.app.taskcards.model.ReminderType.NONE)
            mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, tomorrowTask.id, tomorrow, de.hipp.app.taskcards.model.ReminderType.NONE)
            testDispatcher.scheduler.advanceUntilIdle()

            // Apply today filter
            viewModel.setDueDateRange(DueDateRange.today())
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(1)
            state2.tasks.first().text shouldBe "Due today"
        }
    }

    "due date filter this week shows tasks due within 7 days" {
        runTest(testDispatcher) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 12)
            }.timeInMillis

            val nextWeek = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 5)
                set(Calendar.HOUR_OF_DAY, 12)
            }.timeInMillis

            val nextMonth = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 30)
                set(Calendar.HOUR_OF_DAY, 12)
            }.timeInMillis

            viewModel.add("Due today")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("Due next week")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("Due next month")
            val state1 = waitForState()

            val task1 = state1.tasks.firstOrNull { it.text == "Due today" }
            val task2 = state1.tasks.firstOrNull { it.text == "Due next week" }
            val task3 = state1.tasks.firstOrNull { it.text == "Due next month" }
            task1.shouldNotBeNull()
            task2.shouldNotBeNull()
            task3.shouldNotBeNull()

            mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, task1.id, today, de.hipp.app.taskcards.model.ReminderType.NONE)
            mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, task2.id, nextWeek, de.hipp.app.taskcards.model.ReminderType.NONE)
            mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, task3.id, nextMonth, de.hipp.app.taskcards.model.ReminderType.NONE)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.setDueDateRange(DueDateRange.thisWeek())
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(2)
            state2.tasks.map { it.text }.sorted() shouldContainExactly listOf("Due next week", "Due today")
        }
    }

    "due date filter overdue shows only past due tasks" {
        runTest(testDispatcher) {
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }.timeInMillis

            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis

            viewModel.add("Overdue task")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("Future task")
            val state1 = waitForState()

            val overdueTask = state1.tasks.firstOrNull { it.text == "Overdue task" }
            val futureTask = state1.tasks.firstOrNull { it.text == "Future task" }
            overdueTask.shouldNotBeNull()
            futureTask.shouldNotBeNull()

            mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, overdueTask.id, yesterday, de.hipp.app.taskcards.model.ReminderType.NONE)
            mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, futureTask.id, tomorrow, de.hipp.app.taskcards.model.ReminderType.NONE)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.setDueDateRange(DueDateRange.overdue())
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(1)
            state2.tasks.first().text shouldBe "Overdue task"
        }
    }

    // Combined Filters Tests

    "search and status filter combine with AND logic" {
        runTest(testDispatcher) {
            viewModel.add("Buy groceries")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("Buy tickets")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("Read book")
            val state1 = waitForState()

            val buyTickets = state1.tasks.firstOrNull { it.text == "Buy tickets" }
            buyTickets.shouldNotBeNull()

            viewModel.toggleDone(buyTickets.id, true)
            waitForState()

            // Search for "buy" AND status is ACTIVE_ONLY
            viewModel.setSearchQuery("buy")
            viewModel.setStatusFilter(StatusFilter.ACTIVE_ONLY)
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(1)
            state2.tasks.first().text shouldBe "Buy groceries"
        }
    }

    "search and due date filter combine correctly" {
        runTest(testDispatcher) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 12)
            }.timeInMillis

            viewModel.add("Buy groceries")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.add("Buy tickets")
            val state1 = waitForState()

            val buyGroceries = state1.tasks.firstOrNull { it.text == "Buy groceries" }
            buyGroceries.shouldNotBeNull()

            mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, buyGroceries.id, today, de.hipp.app.taskcards.model.ReminderType.NONE)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.setSearchQuery("buy")
            viewModel.setDueDateRange(DueDateRange.today())
            val state2 = waitForState()

            state2.tasks.shouldHaveSize(1)
            state2.tasks.first().text shouldBe "Buy groceries"
        }
    }

    "clearAllFilters resets all filters" {
        runTest(testDispatcher) {
            viewModel.add("Task 1")
            viewModel.add("Task 2")
            waitForState()

            viewModel.setSearchQuery("test")
            viewModel.setStatusFilter(StatusFilter.ALL)
            viewModel.setDueDateRange(DueDateRange.today())
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.clearAllFilters()
            testDispatcher.scheduler.advanceUntilIdle()

            val filterState = viewModel.searchFilter.first()
            filterState.textQuery shouldBe ""
            filterState.statusFilter shouldBe StatusFilter.ACTIVE_ONLY
            filterState.dueDateRange.shouldBeNull()
        }
    }

    // Error Handling Tests

    "error state exposed when add task fails" {
        runTest(testDispatcher) {
            val failingRepo = FailingTaskListRepository()
            val vmWithFailingRepo = ListViewModel(
                listId = Constants.DEFAULT_LIST_ID,
                repo = failingRepo,
                prefsRepo = mockPrefsRepo,
                strings = mockStrings,
                dispatcher = testDispatcher
            )

            vmWithFailingRepo.add("Test")
            testDispatcher.scheduler.advanceUntilIdle()

            val error = vmWithFailingRepo.errorState.first()
            error.shouldNotBeNull()
            error shouldContain "Error message"
        }
    }

    "error state exposed when remove task fails" {
        runTest(testDispatcher) {
            val failingRepo = FailingTaskListRepository()
            val vmWithFailingRepo = ListViewModel(
                listId = Constants.DEFAULT_LIST_ID,
                repo = failingRepo,
                prefsRepo = mockPrefsRepo,
                strings = mockStrings,
                dispatcher = testDispatcher
            )

            vmWithFailingRepo.remove("task-id")
            testDispatcher.scheduler.advanceUntilIdle()

            val error = vmWithFailingRepo.errorState.first()
            error.shouldNotBeNull()
            error shouldContain "Error message"
        }
    }

    "clearError resets error state" {
        runTest(testDispatcher) {
            val failingRepo = FailingTaskListRepository()
            val vmWithFailingRepo = ListViewModel(
                listId = Constants.DEFAULT_LIST_ID,
                repo = failingRepo,
                prefsRepo = mockPrefsRepo,
                strings = mockStrings,
                dispatcher = testDispatcher
            )

            vmWithFailingRepo.add("Test")
            testDispatcher.scheduler.advanceUntilIdle()

            vmWithFailingRepo.errorState.first().shouldNotBeNull()

            vmWithFailingRepo.clearError()
            testDispatcher.scheduler.advanceUntilIdle()

            val error = vmWithFailingRepo.errorState.first()
            error.shouldBeNull()
        }
    }

    // Saved Search Tests

    "saveCurrentSearch saves search configuration" {
        runTest(testDispatcher) {
            viewModel.setSearchQuery("test")
            viewModel.setStatusFilter(StatusFilter.DONE_ONLY)
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.saveCurrentSearch("My Search")
            testDispatcher.scheduler.advanceUntilIdle()

            val savedSearches = mockPrefsRepo.getSavedSearchesForList(Constants.DEFAULT_LIST_ID).first()
            savedSearches.shouldHaveSize(1)
            savedSearches.first().name shouldBe "My Search"
            savedSearches.first().filter.textQuery shouldBe "test"
            savedSearches.first().filter.statusFilter shouldBe StatusFilter.DONE_ONLY
        }
    }

    "applySavedSearch applies filter configuration" {
        runTest(testDispatcher) {
            val savedSearch = SavedSearch(
                name = "Test Search",
                filter = de.hipp.app.taskcards.model.SearchFilter(
                    textQuery = "buy",
                    statusFilter = StatusFilter.ALL,
                    dueDateRange = DueDateRange.today()
                ),
                listId = Constants.DEFAULT_LIST_ID
            )

            viewModel.applySavedSearch(savedSearch)
            testDispatcher.scheduler.advanceUntilIdle()

            val filterState = viewModel.searchFilter.first()
            filterState.textQuery shouldBe "buy"
            filterState.statusFilter shouldBe StatusFilter.ALL
            filterState.dueDateRange shouldBe DueDateRange.today()
        }
    }

    "deleteSavedSearch removes saved search" {
        runTest(testDispatcher) {
            mockPrefsRepo.saveSavedSearch(
                SavedSearch(
                    id = "search-1",
                    name = "Test",
                    filter = de.hipp.app.taskcards.model.SearchFilter(),
                    listId = Constants.DEFAULT_LIST_ID
                )
            )
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.deleteSavedSearch("search-1")
            testDispatcher.scheduler.advanceUntilIdle()

            val savedSearches = mockPrefsRepo.getSavedSearchesForList(Constants.DEFAULT_LIST_ID).first()
            savedSearches.shouldHaveSize(0)
        }
    }
})

// Mock implementations for testing

private class MockPreferencesRepository : PreferencesRepository {
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

    override fun getSavedSearchesForList(listId: String): Flow<List<SavedSearch>> {
        return flowOf(_savedSearches.value.filter { it.listId == listId })
    }

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

private class MockStringProvider : StringProvider {
    override fun getString(resId: Int): String {
        return "Error message"
    }

    override fun getString(resId: Int, vararg formatArgs: Any): String {
        return "Error message"
    }
}

private class FailingTaskListRepository : TaskListRepository {
    override fun observeTasks(listId: String): Flow<List<de.hipp.app.taskcards.model.TaskItem>> {
        return flowOf(emptyList())
    }

    override suspend fun addTask(listId: String, text: String): de.hipp.app.taskcards.model.TaskItem {
        throw RuntimeException("Test exception")
    }

    override suspend fun removeTask(listId: String, taskId: String) {
        throw RuntimeException("Test exception")
    }

    override suspend fun restoreTask(listId: String, taskId: String) {
        throw RuntimeException("Test exception")
    }

    override suspend fun markDone(listId: String, taskId: String, done: Boolean) {
        throw RuntimeException("Test exception")
    }

    override suspend fun moveTask(listId: String, taskId: String, toIndex: Int) {
        throw RuntimeException("Test exception")
    }

    override suspend fun clearList(listId: String) {
        throw RuntimeException("Test exception")
    }

    override suspend fun updateTaskDueDate(
        listId: String,
        taskId: String,
        dueDate: Long?,
        reminderType: de.hipp.app.taskcards.model.ReminderType
    ) {
        throw RuntimeException("Test exception")
    }

    override suspend fun getActiveListCount(): Int = 0
}
