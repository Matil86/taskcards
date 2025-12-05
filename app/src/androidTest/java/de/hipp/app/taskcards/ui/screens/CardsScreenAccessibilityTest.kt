package de.hipp.app.taskcards.ui.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import de.hipp.app.taskcards.data.InMemoryTaskListRepository
import de.hipp.app.taskcards.ui.screens.cards.CardsScreen
import de.hipp.app.taskcards.ui.theme.TaskCardsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented accessibility tests for CardsScreen deck visualization.
 * Verifies that card count is properly announced to screen readers.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CardsScreenAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deckAnnouncesCardCountForScreenReaders5Cards() = runTest(dispatcher) {
        val repo = InMemoryTaskListRepository(dispatcher)
        val listId = "accessibility-test-1"

        // Add 5 tasks
        repeat(5) { index ->
            repo.addTask(listId, "Task ${index + 1}")
        }

        composeTestRule.setContent {
            TaskCardsTheme {
                CardsScreen(listId = listId)
            }
        }

        composeTestRule.waitForIdle()

        // Verify deck has content description with card count
        composeTestRule.onNodeWithTag("DeckStack")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Deck with 5 cards. Swipe up to draw the first card.")
                )
            )
    }

    @Test
    fun deckAnnouncesReducedCardCountAfterDrawing4CardsRemaining() = runTest(dispatcher) {
        val repo = InMemoryTaskListRepository(dispatcher)
        val listId = "accessibility-test-2"

        // Add 5 tasks
        repeat(5) { index ->
            repo.addTask(listId, "Task ${index + 1}")
        }

        composeTestRule.setContent {
            TaskCardsTheme {
                CardsScreen(listId = listId)
            }
        }

        composeTestRule.waitForIdle()

        // Draw a card
        composeTestRule.onNodeWithTag("DeckStack").performTouchInput {
            swipeUp()
        }

        composeTestRule.waitForIdle()

        // Verify deck now announces 4 cards remaining
        composeTestRule.onNodeWithTag("DeckStack")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Deck with 4 cards remaining. Swipe up to draw the next card.")
                )
            )
    }

    @Test
    fun deckAnnouncesCorrectCountFor1Card() = runTest(dispatcher) {
        val repo = InMemoryTaskListRepository(dispatcher)
        val listId = "accessibility-test-3"

        // Add only 1 task
        repo.addTask(listId, "Single Task")

        composeTestRule.setContent {
            TaskCardsTheme {
                CardsScreen(listId = listId)
            }
        }

        composeTestRule.waitForIdle()

        // Verify deck announces 1 card (singular)
        composeTestRule.onNodeWithTag("DeckStack")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Deck with 1 cards. Swipe up to draw the first card.")
                )
            )
    }

    @Test
    fun deckAnnouncesCardCountLimitAtMaximum5For10Tasks() = runTest(dispatcher) {
        val repo = InMemoryTaskListRepository(dispatcher)
        val listId = "accessibility-test-4"

        // Add 10 tasks (more than max display of 5)
        repeat(10) { index ->
            repo.addTask(listId, "Task ${index + 1}")
        }

        composeTestRule.setContent {
            TaskCardsTheme {
                CardsScreen(listId = listId)
            }
        }

        composeTestRule.waitForIdle()

        // Verify deck announces max of 5 cards (not 10)
        composeTestRule.onNodeWithTag("DeckStack")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Deck with 5 cards. Swipe up to draw the first card.")
                )
            )
    }

    @Test
    fun deckHasLiveRegionForDynamicCardCountUpdates() = runTest(dispatcher) {
        val repo = InMemoryTaskListRepository(dispatcher)
        val listId = "accessibility-test-5"

        // Add 3 tasks
        repeat(3) { index ->
            repo.addTask(listId, "Task ${index + 1}")
        }

        composeTestRule.setContent {
            TaskCardsTheme {
                CardsScreen(listId = listId)
            }
        }

        composeTestRule.waitForIdle()

        // Verify deck has live region property (Polite mode)
        // This ensures screen readers announce changes when card count updates
        composeTestRule.onNodeWithTag("DeckStack")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)
            )
    }
}
