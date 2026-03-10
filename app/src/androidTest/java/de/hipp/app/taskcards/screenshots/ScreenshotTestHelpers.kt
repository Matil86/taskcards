package de.hipp.app.taskcards.screenshots

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Helper utilities for screenshot testing.
 * Provides consistent screenshot naming, storage, and device configuration.
 */
object ScreenshotTestHelpers {

    /**
     * Saves a screenshot to device storage with consistent naming.
     *
     * @param context Android context for accessing file system
     * @param node The Compose node to capture (typically root)
     * @param name Screenshot name (e.g., "01_signin_screen")
     * @param locale Optional locale identifier for localized screenshots
     */
    fun saveScreenshot(
        context: Context,
        node: SemanticsNodeInteraction,
        name: String,
        locale: String? = null
    ) {
        val bitmap = node.captureToImage().asAndroidBitmap()
        saveScreenshot(context, bitmap, name, locale)
    }

    /**
     * Saves a bitmap screenshot to device storage.
     *
     * @param context Android context for accessing file system
     * @param bitmap The bitmap to save
     * @param name Screenshot name
     * @param locale Optional locale identifier for localized screenshots
     */
    fun saveScreenshot(
        context: Context,
        bitmap: Bitmap,
        name: String,
        locale: String? = null
    ) {
        val screenshotDir = getScreenshotDirectory(context, locale)
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs()
        }

        val filename = if (locale != null) {
            "${name}_${locale}.png"
        } else {
            "$name.png"
        }

        val file = File(screenshotDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Print to logcat for easy debugging
        println("Screenshot saved: ${file.absolutePath}")
    }

    /**
     * Gets the screenshot directory for the current test run.
     * Organizes screenshots by locale if specified.
     */
    private fun getScreenshotDirectory(context: Context, locale: String?): File {
        val baseDir = File(context.getExternalFilesDir(null), "screenshots")
        return if (locale != null) {
            File(baseDir, locale)
        } else {
            baseDir
        }
    }

    /**
     * Device information for screenshot metadata.
     */
    data class DeviceInfo(
        val model: String,
        val manufacturer: String,
        val androidVersion: Int,
        val screenWidth: Int,
        val screenHeight: Int,
        val density: Float
    )

    /**
     * Gets current device information for screenshot metadata.
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        val displayMetrics = context.resources.displayMetrics
        return DeviceInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.SDK_INT,
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels,
            density = displayMetrics.density
        )
    }

    /**
     * Changes the app locale for localized screenshots.
     * Note: This may require app restart in production, but works in tests.
     *
     * @param context Android context
     * @param languageCode Language code (e.g., "en", "de", "ja")
     */
    @Suppress("DEPRECATION")
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Supported languages for screenshot generation.
     */
    enum class SupportedLanguage(val code: String, val displayName: String) {
        ENGLISH("en", "English"),
        GERMAN("de", "Deutsch"),
        JAPANESE("ja", "日本語")
    }
}

/**
 * Sample task data for screenshot tests.
 * Provides consistent, realistic task examples that showcase app features.
 */
object ScreenshotSampleData {

    /**
     * Task templates organized by theme for variety.
     */
    data class TaskTemplate(
        val text: String,
        val hasDueDate: Boolean = false,
        val daysUntilDue: Int = 0,
        val hasReminder: Boolean = false,
        val isCompleted: Boolean = false
    )

    /**
     * Work-themed tasks for professional context.
     */
    val workTasks = listOf(
        TaskTemplate("Finish quarterly report", hasDueDate = true, daysUntilDue = 1, hasReminder = true),
        TaskTemplate("Review team feedback", isCompleted = true),
        TaskTemplate("Schedule client meeting", hasDueDate = true, daysUntilDue = 7),
        TaskTemplate("Update project documentation", hasDueDate = true, daysUntilDue = 0, hasReminder = true),
        TaskTemplate("Prepare presentation slides")
    )

    /**
     * Personal tasks for everyday life context.
     */
    val personalTasks = listOf(
        TaskTemplate("Buy groceries for dinner party", hasDueDate = true, daysUntilDue = 0, hasReminder = true),
        TaskTemplate("Call mom to wish happy birthday", hasDueDate = true, daysUntilDue = 2),
        TaskTemplate("Plan weekend hiking trip"),
        TaskTemplate("Schedule dentist appointment", hasDueDate = true, daysUntilDue = 7),
        TaskTemplate("Submit expense reports", isCompleted = true)
    )

    /**
     * Mixed tasks showing variety of features.
     */
    val mixedTasks = listOf(
        TaskTemplate("Review Pull Request #234", hasDueDate = true, daysUntilDue = 0),
        TaskTemplate("Learn new framework concepts"),
        TaskTemplate("Fix critical bug in production", hasDueDate = true, daysUntilDue = 1, hasReminder = true),
        TaskTemplate("Refactor authentication module", isCompleted = true),
        TaskTemplate("Write unit tests for new feature"),
        TaskTemplate("Deploy to staging environment", hasDueDate = true, daysUntilDue = 3)
    )

    /**
     * Tasks with emojis to showcase Unicode support.
     */
    val emojiTasks = listOf(
        TaskTemplate("🍕 Order pizza for team lunch", hasDueDate = true, daysUntilDue = 0),
        TaskTemplate("✈️ Book flight tickets", hasDueDate = true, daysUntilDue = 5, hasReminder = true),
        TaskTemplate("🎂 Bake birthday cake", hasDueDate = true, daysUntilDue = 2),
        TaskTemplate("🏃 Morning run completed", isCompleted = true),
        TaskTemplate("📚 Read new book chapter")
    )
}
