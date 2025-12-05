package de.hipp.app.taskcards.ui.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
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
import java.io.File

/**
 * Instrumented Compose UI test for SettingsScreen.
 * Tests rendering and basic toggle functionality for high contrast mode on device/emulator.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsScreenBasicTest : StringSpec() {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context

    init {
        beforeSpec {
            context = ApplicationProvider.getApplicationContext()
        }

        beforeTest {
            Dispatchers.setMain(dispatcher)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        "displays settings screen with all UI elements" {
            runTest(dispatcher) {
                // Clean up DataStore files before test
                val dataStoreFile = File(context.filesDir, "datastore/settings.preferences_pb")
                dataStoreFile.delete()

                val prefsRepo = PreferencesRepository(context)

                // Verify initial state is off
                prefsRepo.highContrastMode.first() shouldBe false

                composeTestRule.setContent {
                    TaskCardsTheme {
                        SettingsScreen()
                    }
                }

                composeTestRule.waitForIdle()

                // Verify screen title is displayed
                composeTestRule.onNodeWithText("Settings", substring = true).assertIsDisplayed()

                // Verify high contrast mode setting is displayed
                composeTestRule.onNodeWithText("High Contrast Mode", substring = true).assertIsDisplayed()

                // Verify description is displayed
                composeTestRule.onNodeWithText("Improve visibility", substring = true).assertIsDisplayed()

                // Verify app version is displayed
                composeTestRule.onNodeWithText("Version", substring = true).assertIsDisplayed()
            }
        }
    }
}
