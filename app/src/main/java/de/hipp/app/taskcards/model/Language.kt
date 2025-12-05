package de.hipp.app.taskcards.model

/**
 * Represents a language option for the app.
 *
 * @param code ISO language code ("en", "de", "ja") or "system" for device default
 * @param flag Unicode flag emoji representing the language
 * @param nativeName The name of the language in its native script
 */
data class Language(
    val code: String,
    val flag: String,
    val nativeName: String
) {
    companion object {
        /**
         * All available language options including system default.
         */
        fun getAllLanguages(): List<Language> = listOf(
            Language("system", "🌐", "System Default"),
            Language("en", "🇺🇸", "English"),
            Language("de", "🇩🇪", "Deutsch"),
            Language("ja", "🇯🇵", "日本語")
        )

        /**
         * Gets a Language by its code, defaulting to system if not found.
         */
        fun fromCode(code: String): Language {
            return getAllLanguages().find { it.code == code }
                ?: Language("system", "🌐", "System Default")
        }
    }
}
