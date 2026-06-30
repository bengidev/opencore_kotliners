package io.github.bengidev.opencore.speech.utilities

import androidx.core.os.LocaleListCompat
import java.util.Locale

/** Picks the best available recognizer locale without hardcoding a single language. */
internal object SpeechRecognizerLocaleResolver {
    fun resolve(
        preferred: Locale = Locale.getDefault(),
        isAvailable: (Locale) -> Boolean,
    ): Locale? {
        if (isAvailable(preferred)) {
            return preferred
        }

        val languageOnly = Locale.forLanguageTag(preferred.language)
        if (languageOnly != preferred && isAvailable(languageOnly)) {
            return languageOnly
        }

        val preferredLocales = LocaleListCompat.getAdjustedDefault()
        for (index in 0 until preferredLocales.size()) {
            val locale = preferredLocales.get(index) ?: continue
            if (isAvailable(locale)) {
                return locale
            }
        }

        val english = Locale.forLanguageTag("en-US")
        if (isAvailable(english)) {
            return english
        }

        return null
    }
}
