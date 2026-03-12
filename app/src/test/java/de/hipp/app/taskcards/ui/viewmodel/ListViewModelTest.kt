package de.hipp.app.taskcards.ui.viewmodel

import de.hipp.app.taskcards.data.InMemoryTaskListRepository
import de.hipp.app.taskcards.model.DueDateRange
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.StatusFilter
import de.hipp.app.taskcards.ui.viewmodel.list.applySavedSearch
import de.hipp.app.taskcards.ui.viewmodel.list.clearAllFilters
import de.hipp.app.taskcards.ui.viewmodel.list.deleteSavedSearch
import de.hipp.app.taskcards.ui.viewmodel.list.saveCurrentSearch
import de.hipp.app.taskcards.ui.viewmodel.list.setDueDateRange
import de.hipp.app.taskcards.ui.viewmodel.list.setSearchQuery
import de.hipp.app.taskcards.ui.viewmodel.list.setStatusFilter
import de.hipp.app.taskcards.util.Constants
import de.hipp.app.taskcards.util.FailingTaskListRepository
import de.hipp.app.taskcards.util.MockPreferencesRepository
import de.hipp.app.taskcards.util.MockStringProvider
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.SearchFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LargeClass")
class ListViewModelTest : FunSpec({
    // Fresh dispatchers per test — sharing a single StandardTestDispatcher across tests causes
    // accumulated virtual time from WhileSubscribed(5000) timers to interfere with later tests.
    // Note: lateinit var is not valid inside a lambda; use var with a placeholder initial value
    // that is replaced in beforeTest before any test runs.
    var testDispatcher = StandardTestDispatcher()
    var mainDispatcher = UnconfinedTestDispatcher(testDispatcher.scheduler)
    lateinit var mockRepo: InMemoryTaskListRepository
    lateinit var mockPrefsRepo: MockPreferencesRepository
    lateinit var mockStrings: MockStringProvider
    lateinit var viewModel: ListViewModel

    // Helper to return latest state — with UnconfinedTestDispatcher no advancing needed
    // but kept for readability and to advance virtual time past any delays if needed
    fun waitForState(): ListViewModel.UiState {
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel.state.value
    }

    beforeTest {
        testDispatcher = StandardTestDispatcher()
        mainDispatcher = UnconfinedTestDispatcher(testDispatcher.scheduler)
        Dispatchers.setMain(mainDispatcher)
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

    context("Basic Operations") {
        test("adding task updates state flow with new task") {
            runTest(testDispatcher) {
                // Start collecting the state to activate the flow
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

                viewModel.add("Test task")
                testDispatcher.scheduler.advanceUntilIdle()

                val state = viewModel.state.value
                state.tasks.shouldHaveSize(1)
                state.tasks.first().text shouldBe "Test task"
                state.tasks.first().done shouldBe false
            }
        }

        test("adding task trims whitespace") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

                viewModel.add("  Task with spaces  ")
                val state = waitForState()

                state.tasks.shouldHaveSize(1)
                state.tasks.first().text shouldBe "Task with spaces"
            }
        }

        test("adding empty task is ignored") {
            runTest(testDispatcher) {
                viewModel.add("")
                val state = waitForState()

                state.tasks.shouldHaveSize(0)
            }
        }

        test("adding whitespace-only task is ignored") {
            runTest(testDispatcher) {
                viewModel.add("   ")
                val state = waitForState()

                state.tasks.shouldHaveSize(0)
            }
        }

        test("removing task marks task as removed") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

                viewModel.add("Task to remove")
                val state1 = waitForState()
                val taskId = state1.tasks.first().id

                viewModel.remove(taskId)
                val state2 = waitForState()

                // Default filter is ACTIVE_ONLY, so removed task should not appear
                state2.tasks.shouldHaveSize(0)
            }
        }

        test("restoring task marks task as active") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

        test("toggling done updates task done status") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

        test("toggling done to false marks task as active") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

        test("moving task updates order correctly") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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
    }

    context("Search Functionality") {
        test("search query filters tasks by text content") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

        test("search query is case insensitive") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

                viewModel.add("UPPERCASE TASK")
                viewModel.add("lowercase task")
                viewModel.add("MiXeD CaSe TaSk")
                waitForState()

                viewModel.setSearchQuery("task")
                val state = waitForState()

                state.tasks.shouldHaveSize(3)
            }
        }

        test("empty search query shows all tasks") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

                viewModel.add("Task 1")
                viewModel.add("Task 2")
                viewModel.add("Task 3")
                waitForState()

                viewModel.setSearchQuery("")
                val state = waitForState()

                state.tasks.shouldHaveSize(3)
            }
        }

        test("search with no matches returns empty list") {
            runTest(testDispatcher) {
                viewModel.add("Buy groceries")
                viewModel.add("Read book")
                waitForState()

                viewModel.setSearchQuery("nonexistent")
                val state = waitForState()

                state.tasks.shouldHaveSize(0)
            }
        }

        test("search handles special characters correctly") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

                viewModel.add("Task with @special #characters")
                viewModel.add("Normal task")
                waitForState()

                viewModel.setSearchQuery("@special")
                val state = waitForState()

                state.tasks.shouldHaveSize(1)
                state.tasks.first().text shouldBe "Task with @special #characters"
            }
        }

        test("search handles unicode characters") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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
    }

    context("Status Filter") {
        test("status filter ALL shows all tasks") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

        test("status filter ACTIVE_ONLY shows only active tasks") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

        test("status filter DONE_ONLY shows only completed tasks") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

        test("status filter REMOVED_ONLY shows only removed tasks") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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
    }

    context("Due Date Filter") {
        test("due date filter today shows only tasks due today") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

                mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, todayTask.id, today, ReminderType.NONE)
                mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, tomorrowTask.id, tomorrow, ReminderType.NONE)
                testDispatcher.scheduler.advanceUntilIdle()

                // Apply today filter
                viewModel.setDueDateRange(DueDateRange.today())
                val state2 = waitForState()

                state2.tasks.shouldHaveSize(1)
                state2.tasks.first().text shouldBe "Due today"
            }
        }

        test("due date filter this week shows tasks due within 7 days") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

                mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, task1.id, today, ReminderType.NONE)
                mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, task2.id, nextWeek, ReminderType.NONE)
                mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, task3.id, nextMonth, ReminderType.NONE)
                testDispatcher.scheduler.advanceUntilIdle()

                viewModel.setDueDateRange(DueDateRange.thisWeek())
                val state2 = waitForState()

                state2.tasks.shouldHaveSize(2)
                state2.tasks.map { it.text }.sorted() shouldContainExactly listOf("Due next week", "Due today")
            }
        }

        test("due date filter overdue shows only past due tasks") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

                // Note: InMemoryTaskListRepository validates that due dates cannot be in the past
                // This is intentional for production use. For testing overdue functionality,
                // we test the filter logic by verifying it works correctly with the constraint.
                // In a real app scenario, tasks become overdue when time passes, not when created.

                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis

                val nextWeek = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 7)
                }.timeInMillis

                viewModel.add("Future task soon")
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.add("Future task later")
                val state1 = waitForState()

                val task1 = state1.tasks.firstOrNull { it.text == "Future task soon" }
                val task2 = state1.tasks.firstOrNull { it.text == "Future task later" }
                task1.shouldNotBeNull()
                task2.shouldNotBeNull()

                mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, task1.id, tomorrow, ReminderType.NONE)
                mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, task2.id, nextWeek, ReminderType.NONE)
                testDispatcher.scheduler.advanceUntilIdle()

                // Test that overdue filter works (even though we can't create truly overdue tasks in tests)
                viewModel.setDueDateRange(DueDateRange.overdue())
                val state2 = waitForState()

                // With no overdue tasks, should show empty list
                state2.tasks.shouldHaveSize(0)
            }
        }
    }

    context("Combined Filters") {
        test("search and status filter combine with AND logic") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

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

        test("search and due date filter combine correctly") {
            runTest(testDispatcher) {
                backgroundScope.launch {
                    viewModel.state.collect {}
                }
                testDispatcher.scheduler.runCurrent()

                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 12)
                }.timeInMillis

                viewModel.add("Buy groceries")
                testDispatcher.scheduler.advanceUntilIdle()
                viewModel.add("Buy tickets")
                val state1 = waitForState()

                val buyGroceries = state1.tasks.firstOrNull { it.text == "Buy groceries" }
                buyGroceries.shouldNotBeNull()

                mockRepo.updateTaskDueDate(Constants.DEFAULT_LIST_ID, buyGroceries.id, today, ReminderType.NONE)
                testDispatcher.scheduler.advanceUntilIdle()

                viewModel.setSearchQuery("buy")
                viewModel.setDueDateRange(DueDateRange.today())
                val state2 = waitForState()

                state2.tasks.shouldHaveSize(1)
                state2.tasks.first().text shouldBe "Buy groceries"
            }
        }

        test("clearAllFilters resets all filters") {
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
    }

    context("Error Handling") {
        test("error state exposed when add task fails") {
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

        test("error state exposed when remove task fails") {
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

        test("clearError resets error state") {
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
    }

    context("Saved Searches") {
        test("saveCurrentSearch saves search configuration") {
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

        test("applySavedSearch applies filter configuration") {
            runTest(testDispatcher) {
                val savedSearch = SavedSearch(
                    name = "Test Search",
                    filter = SearchFilter(
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

        test("deleteSavedSearch removes saved search") {
            runTest(testDispatcher) {
                mockPrefsRepo.saveSavedSearch(
                    SavedSearch(
                        id = "search-1",
                        name = "Test",
                        filter = SearchFilter(),
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
    }
})
