package com.penny.wallet.platform

import com.penny.wallet.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Shortcuts and languages live in resource files the compiler can't check against the code. */
class AppEntryPointsTest {
    private fun file(path: String) = File(path).readText()

    @Test fun shortcutsUseTheActionsMainActivityHandles() {
        val actions = Regex("android:action=\"([^\"]+)\"").findAll(file("src/main/res/xml/shortcuts.xml")).map { it.groupValues[1] }.toSet()
        assertEquals(setOf(MainActivity.ACTION_QUICK_ADD, MainActivity.ACTION_SCAN_RECEIPT), actions)
    }

    @Test fun smokeShortcutsDifferOnlyInPackage() {
        fun body(path: String) = file(path).lines().filterNot { it.trim().startsWith("<!--") }.joinToString("\n")
        val main = body("src/main/res/xml/shortcuts.xml")
        val smoke = body("src/smoke/res/xml/shortcuts.xml")
        assertEquals(main.replace("\"com.penny.wallet\"", "\"com.penny.wallet.smoke\""), smoke)
    }

    @Test fun languageListMatchesBuildAndTranslations() {
        val filters = Regex("localeFilters \\+= listOf\\(([^)]*)\\)").find(file("build.gradle.kts"))!!.groupValues[1]
            .split(",").map { it.trim().trim('"') }.toSet()
        val translated = File("src/main/res").listFiles()!!.map { it.name }
            .filter { it.matches(Regex("values-[a-z]{2}")) }.map { it.removePrefix("values-") }.toSet() + "en"
        assertEquals(filters, AppLanguage.tags.filter { it.isNotEmpty() }.toSet())
        assertEquals(filters, translated)
    }

    @Test fun languagesAreNamedInTheirOwnWords() {
        assertEquals(listOf("English", "Deutsch", "Italiano"), listOf("en", "de", "it").map(AppLanguage::nativeName))
    }
}
