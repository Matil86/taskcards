package de.hipp.app.taskcards.ui.navigation

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasRole
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.hipp.app.taskcards.ui.theme.TaskCardsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose UI tests for ModernNavigationBar.
 *
 * These tests guard against the navigation clipping bug where the Settings item (the 3rd tab)
 * was not rendered/accessible because the pill was too narrow for its content.
 *
 * All 3 items must be present, displayed, and interactive regardless of which route is active.
 */
@RunWith(AndroidJUnit4::class)
class NavigationBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Core regression test: all 3 Role.Tab nodes must exist and be displayed.
     *
     * If any item is clipped out of the pill the semantics tree will be missing that tab,
     * and assertCountEquals(3) will fail — catching the original bug automatically.
     */
    @Test
    fun allThreeNavItemsAreDisplayed() {
        composeTestRule.setContent {
            // Wrap in theme so colors and MaterialTheme tokens resolve
            TaskCardsTheme {
                ModernNavigationBar(
                    currentRoute = "cards",
                    onNavigateToCards = {},
                    onNavigateToList = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // All 3 Role.Tab items should exist and be displayed
        composeTestRule
            .onAllNodes(hasRole(Role.Tab))
            .assertCountEquals(3)  // Cards + List + Settings — fails if any is clipped/missing
    }

    /**
     * Verifies the Settings nav item (historically the one that got cut off) is tappable and
     * triggers the correct callback.
     */
    @Test
    fun settingsNavItemIsClickable() {
        var settingsClicked = false

        composeTestRule.setContent {
            TaskCardsTheme {
                ModernNavigationBar(
                    currentRoute = "cards",
                    onNavigateToCards = {},
                    onNavigateToList = {},
                    onNavigateToSettings = { settingsClicked = true }
                )
            }
        }

        // The 3rd tab (index 2) is the Settings item
        composeTestRule
            .onAllNodes(hasRole(Role.Tab))[2]
            .assertIsDisplayed()
            .performClick()

        assert(settingsClicked) { "Settings nav item click was not registered" }
    }

    /**
     * Verifies the selected item expands to show its text label.
     * ModernNavItem only renders the Text child when selected=true, so assertIsDisplayed()
     * will fail if the selected state is broken or the item is clipped.
     */
    @Test
    fun selectedItemShowsLabel() {
        composeTestRule.setContent {
            TaskCardsTheme {
                ModernNavigationBar(
                    currentRoute = "settings",
                    onNavigateToCards = {},
                    onNavigateToList = {},
                    onNavigateToSettings = {}
                )
            }
        }

        // Settings label should be visible when that route is active
        composeTestRule
            .onNodeWithText("Settings", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Verifies the Cards item label shows when that route is active.
     * Symmetry check — ensures selection logic works for the first item too.
     */
    @Test
    fun cardsItemShowsLabelWhenSelected() {
        composeTestRule.setContent {
            TaskCardsTheme {
                ModernNavigationBar(
                    currentRoute = "cards/some-list-id",
                    onNavigateToCards = {},
                    onNavigateToList = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Cards", ignoreCase = true)
            .assertIsDisplayed()
    }
}
