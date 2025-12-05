package de.hipp.app.taskcards.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import de.hipp.app.taskcards.data.InMemoryTaskListRepository
import de.hipp.app.taskcards.ui.theme.TaskCardsTheme
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule

/**
 * Instrumented Compose UI tests for ListScreen.
 * Tests critical rendering and interaction flows on device/emulator.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ListScreenBasicTest : StringSpec() {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        "renders task list with add input and displays tasks" {
            runTest(dispatcher) {
                val repo = InMemoryTaskListRepository(dispatcher)
                val listId = "list-test-1"

                // Setup: Add some tasks
                repo.addTask(listId, "Buy groceries")
                repo.addTask(listId, "Write tests")
                repo.addTask(listId, "Review PR")

                composeTestRule.setContent {
                    TaskCardsTheme {
                        ListScreen(listId = listId, onLoadList = {})
                    }
                }

                composeTestRule.waitForIdle()

                // Verify tasks are displayed (newest first due to order)
                composeTestRule.onNodeWithText("Review PR").assertIsDisplayed()
                composeTestRule.onNodeWithText("Write tests").assertIsDisplayed()
                composeTestRule.onNodeWithText("Buy groceries").assertIsDisplayed()

                // Verify add input field exists
                composeTestRule.onNodeWithTag("AddTaskInput").assertExists()
            }
        }

        "allows adding a new task through input field" {
            runTest(dispatcher) {
                val repo = InMemoryTaskListRepository(dispatcher)
                val listId = "list-test-2"

                composeTestRule.setContent {
                    TaskCardsTheme {
                        ListScreen(listId = listId, onLoadList = {})
                    }
                }

                composeTestRule.waitForIdle()

                // Enter text in input field
                composeTestRule.onNodeWithTag("AddTaskInput").performTextInput("New task item")

                // Click add button
                composeTestRule.onNodeWithContentDescription("Add task", substring = true).performClick()

                composeTestRule.waitForIdle()

                // Verify task was added to repository
                runTest {
                    val taskList = repo.observeTasks(listId).first()
                    taskList.size shouldBe 1
                    taskList.first().text shouldBe "New task item"
                }
            }
        }

        "displays checkboxes for toggling task completion" {
            runTest(dispatcher) {
                val repo = InMemoryTaskListRepository(dispatcher)
                val listId = "list-test-3"

                // Setup: Add a task
                val task = repo.addTask(listId, "Complete this task")

                composeTestRule.setContent {
                    TaskCardsTheme {
                        ListScreen(listId = listId, onLoadList = {})
                    }
                }

                composeTestRule.waitForIdle()

                // Verify task is displayed
                composeTestRule.onNodeWithText("Complete this task").assertIsDisplayed()

                // Find and verify checkbox exists for the task
                // The checkbox should be unchecked initially
                composeTestRule.onNodeWithTag("TaskCheckbox-${task.id}").assertExists()

                // Click the checkbox to mark as done
                composeTestRule.onNodeWithTag("TaskCheckbox-${task.id}").performClick()

                composeTestRule.waitForIdle()

                // Verify task is marked as done in repository
                runTest {
                    val taskList = repo.observeTasks(listId).first()
                    val updatedTask = taskList.find { it.id == task.id }
                    updatedTask?.done shouldBe true
                }
            }
        }
    }
}
