package de.hipp.app.taskcards.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * Utility object for managing app locale and language settings.
 *
 * This helper provides runtime locale switching without requiring app restart.
 * It wraps the context with the selected locale configuration.
 */
object LocaleHelper {

    /**
     * Sets the app locale based on the language code.
     *
     * @param context The current context
     * @param languageCode ISO language code ("en", "de", "ja") or "system" for device default
     * @return A new context wrapped with the selected locale configuration
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = getLocaleFromCode(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Gets the Locale object for a given language code.
     *
     * @param languageCode ISO language code ("en", "de", "ja") or "system"
     * @return The corresponding Locale object
     */
    fun getLocaleFromCode(languageCode: String): Locale {
        return when (languageCode) {
            "de" -> Locale.forLanguageTag("de")
            "ja" -> Locale.forLanguageTag("ja")
            "en" -> Locale.forLanguageTag("en")
            "system" -> {
                // Use Resources.getSystem() for the TRUE device locale.
                // Locale.getDefault() is unreliable here: setLocale() calls
                // Locale.setDefault() which overwrites the JVM default, so
                // getDefault() would return our previously selected language
                // instead of the actual system locale.
                // Resources.getSystem() reads Android's own configuration
                // which is never affected by Locale.setDefault().
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Resources.getSystem().configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    Resources.getSystem().configuration.locale
                }
            }
            else -> Locale.ENGLISH // Fallback to English for unsupported locales
        }
    }

    /**
     * Gets the current language code from the context configuration.
     *
     * @param context The current context
     * @return The ISO language code ("en", "de", "ja") or "en" as fallback
     */
    fun getCurrentLanguageCode(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        return when (locale.language) {
            "de" -> "de"
            "ja" -> "ja"
            "en" -> "en"
            else -> "en" // Fallback to English
        }
    }

    /**
     * Checks if the device supports the given language code.
     *
     * @param languageCode The language code to check
     * @return True if supported, false otherwise
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return languageCode in listOf("en", "de", "ja", "system")
    }
}
