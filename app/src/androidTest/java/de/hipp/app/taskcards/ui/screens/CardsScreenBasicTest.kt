package de.hipp.app.taskcards.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import de.hipp.app.taskcards.data.InMemoryTaskListRepository
import de.hipp.app.taskcards.ui.theme.TaskCardsTheme
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule

/**
 * Instrumented Compose UI tests for CardsScreen.
 * Tests critical rendering and interaction flows on device/emulator.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CardsScreenBasicTest : StringSpec() {

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

        "renders deck stack with tasks and completion message when empty" {
            runTest(dispatcher) {
                val repo = InMemoryTaskListRepository(dispatcher)
                val listId = "cards-test-1"

                // Setup: Add tasks to the repository
                repo.addTask(listId, "Task 1")
                repo.addTask(listId, "Task 2")
                repo.addTask(listId, "Task 3")

                composeTestRule.setContent {
                    TaskCardsTheme {
                        CardsScreen(listId = listId)
                    }
                }

                // Verify deck stack is displayed
                composeTestRule.onNodeWithTag("DeckStack").assertIsDisplayed()

                // Verify deck layers are rendered (we should see the cards sticking out)
                composeTestRule.onNodeWithTag("DeckLayer").assertExists()

                // Draw a card by swiping up on deck
                composeTestRule.onNodeWithTag("DeckStack").performTouchInput {
                    swipeUp()
                }

                // Wait for animation and state update
                composeTestRule.waitForIdle()

                // Verify top card is now displayed
                composeTestRule.onNodeWithTag("TopCard").assertIsDisplayed()

                // Verify task text is visible (latest task is shown first due to order)
                composeTestRule.onNodeWithText("Task 3").assertIsDisplayed()
            }
        }

        "displays completion celebration when all tasks are done" {
            runTest(dispatcher) {
                val repo = InMemoryTaskListRepository(dispatcher)
                val listId = "cards-test-2"

                // Setup: Start with no tasks

                composeTestRule.setContent {
                    TaskCardsTheme {
                        CardsScreen(listId = listId)
                    }
                }

                composeTestRule.waitForIdle()

                // Verify completion message is displayed when no tasks exist
                composeTestRule.onNodeWithText("You did all the tasks.").assertIsDisplayed()
                composeTestRule.onNodeWithText("Nice!!!").assertIsDisplayed()
                composeTestRule.onNodeWithText(
                    "Add more tasks in the list to keep the momentum going!",
                    substring = true
                ).assertIsDisplayed()
            }
        }
    }
}
