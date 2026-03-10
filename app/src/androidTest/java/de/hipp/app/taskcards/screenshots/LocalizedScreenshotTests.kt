package de.hipp.app.taskcards.screenshots

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import de.hipp.app.taskcards.data.InMemoryTaskListRepository
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.ui.screens.cards.CardsScreen
import de.hipp.app.taskcards.ui.screens.list.ListScreen
import de.hipp.app.taskcards.ui.screens.settings.SettingsScreen
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
import java.util.Calendar

/**
 * Localized screenshot generation tests for Play Store listings in multiple languages.
 *
 * This test suite generates screenshots for all supported languages (English, German, Japanese)
 * to support international Play Store listings.
 *
 * ## Running Localized Screenshots
 *
 * To generate screenshots for all languages:
 * ```
 * ./gradlew connectedAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=de.hipp.app.taskcards.screenshots.LocalizedScreenshotTests
 * ```
 *
 * ## Output Structure
 *
 * Screenshots are organized by language:
 * ```
 * /sdcard/Android/data/de.hipp.app.taskcards/files/screenshots/
 *   ├── en/
 *   │   ├── 01_cards_screen.png
 *   │   └── ...
 *   ├── de/
 *   │   ├── 01_cards_screen.png
 *   │   └── ...
 *   └── ja/
 *       ├── 01_cards_screen.png
 *       └── ...
 * ```
 *
 * ## Pulling Screenshots
 *
 * ```bash
 * adb pull /sdcard/Android/data/de.hipp.app.taskcards/files/screenshots ./screenshots
 * ```
 *
 * This creates Play Store ready screenshots for each language market.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LocalizedScreenshotTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    /**
     * Helper to create sample tasks with due dates.
     */
    private suspend fun createSampleTasks(repo: InMemoryTaskListRepository, listId: String) {
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val tomorrow = calendar.apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        val nextWeek = calendar.apply {
            add(Calendar.DAY_OF_MONTH, 6)
        }.timeInMillis

        // Use sample data templates
        val templates = ScreenshotSampleData.personalTasks.take(5)

        templates.forEachIndexed { index, template ->
            val task = repo.addTask(listId, template.text)

            if (template.hasDueDate) {
                val dueDate = when (template.daysUntilDue) {
                    0 -> today
                    1 -> tomorrow
                    else -> nextWeek
                }
                val reminderType = if (template.hasReminder) {
                    when (template.daysUntilDue) {
                        0 -> ReminderType.ON_DUE_DATE
                        1 -> ReminderType.ONE_DAY_BEFORE
                        else -> ReminderType.ONE_WEEK_BEFORE
                    }
                } else {
                    ReminderType.NONE
                }
                repo.updateTaskDueDate(listId, task.id, dueDate, reminderType)
            }

            if (template.isCompleted) {
                repo.markDone(listId, task.id, true)
            }
        }
    }

    /**
     * Generate localized screenshots for all supported languages.
     * This is a comprehensive test that captures key screens in each language.
     */
    @Test
    fun generateAllLocalizedScreenshots() = runTest(dispatcher) {
        ScreenshotTestHelpers.SupportedLanguage.entries.forEach { language ->
            println("Generating screenshots for ${language.displayName} (${language.code})")

            // Set locale for this language
            val localizedContext = ScreenshotTestHelpers.setLocale(context, language.code)

            // Generate screenshots for this language
            generateCardsScreenshot(language.code)
            generateListScreenshot(language.code)
            generateSettingsScreenshot(language.code)

            println("Completed screenshots for ${language.displayName}")
        }
    }

    /**
     * Generate Cards screen screenshot in specified language.
     */
    private fun generateCardsScreenshot(languageCode: String) = runTest(dispatcher) {
        val repo = InMemoryTaskListRepository(dispatcher)
        val listId = "screenshot-cards-$languageCode"

        createSampleTasks(repo, listId)

        composeTestRule.setContent {
            TaskCardsTheme {
                CardsScreen(listId = listId)
            }
        }

        composeTestRule.waitForIdle()

        // Capture deck view
        ScreenshotTestHelpers.saveScreenshot(
            context,
            composeTestRule.onRoot(),
            "01_cards_deck",
            languageCode
        )

        // Draw a card
        composeTestRule.onNodeWithTag("DeckStack").performTouchInput {
            swipeUp()
        }

        composeTestRule.waitForIdle()

        // Capture with card drawn
        ScreenshotTestHelpers.saveScreenshot(
            context,
            composeTestRule.onRoot(),
            "02_cards_drawn",
            languageCode
        )
    }

    /**
     * Generate List screen screenshot in specified language.
     */
    private fun generateListScreenshot(languageCode: String) = runTest(dispatcher) {
        val repo = InMemoryTaskListRepository(dispatcher)
        val listId = "screenshot-list-$languageCode"

        createSampleTasks(repo, listId)

        composeTestRule.setContent {
            TaskCardsTheme {
                ListScreen(
                    listId = listId,
                    onLoadList = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Capture list view
        ScreenshotTestHelpers.saveScreenshot(
            context,
            composeTestRule.onRoot(),
            "03_list_screen",
            languageCode
        )
    }

    /**
     * Generate Settings screen screenshot in specified language.
     */
    private fun generateSettingsScreenshot(languageCode: String) = runTest(dispatcher) {
        composeTestRule.setContent {
            TaskCardsTheme {
                SettingsScreen(
                    isAuthenticated = false,
                    userEmail = null,
                    onSignInClick = {},
                    onSignOutClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Capture settings
        ScreenshotTestHelpers.saveScreenshot(
            context,
            composeTestRule.onRoot(),
            "04_settings_screen",
            languageCode
        )
    }

    // Individual language tests for easier debugging

    @Test
    fun generateEnglishScreenshots() = runTest(dispatcher) {
        val language = ScreenshotTestHelpers.SupportedLanguage.ENGLISH
        ScreenshotTestHelpers.setLocale(context, language.code)

        generateCardsScreenshot(language.code)
        generateListScreenshot(language.code)
        generateSettingsScreenshot(language.code)
    }

    @Test
    fun generateGermanScreenshots() = runTest(dispatcher) {
        val language = ScreenshotTestHelpers.SupportedLanguage.GERMAN
        ScreenshotTestHelpers.setLocale(context, language.code)

        generateCardsScreenshot(language.code)
        generateListScreenshot(language.code)
        generateSettingsScreenshot(language.code)
    }

    @Test
    fun generateJapaneseScreenshots() = runTest(dispatcher) {
        val language = ScreenshotTestHelpers.SupportedLanguage.JAPANESE
        ScreenshotTestHelpers.setLocale(context, language.code)

        generateCardsScreenshot(language.code)
        generateListScreenshot(language.code)
        generateSettingsScreenshot(language.code)
    }
}
