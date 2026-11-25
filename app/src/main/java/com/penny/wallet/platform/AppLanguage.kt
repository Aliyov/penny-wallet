package com.penny.wallet.platform

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * The app's own language, independent of the phone's. Offered on Android 13+ only: there the system applies it to the
 * whole process, so notifications, widgets and texts built from the application context follow it too. Below 13
 * AppCompat would only translate the activity and leave those in the phone's language.
 */
object AppLanguage {
    val supported: Boolean get() = Build.VERSION.SDK_INT >= 33

    /** "" = follow the phone. Must match `localeFilters` in app/build.gradle.kts (checked by a test). */
    val tags = listOf("", "en", "de", "it", "fr", "es", "pt", "pl", "tr")

    fun current(): String {
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.language.orEmpty()
        return if (tag in tags) tag else ""
    }

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    /** Each language in its own words ("Deutsch", "Italiano"), as people look for their own. */
    fun nativeName(tag: String): String {
        val l = Locale.forLanguageTag(tag)
        return l.getDisplayLanguage(l).replaceFirstChar { it.titlecase(l) }
    }
}
