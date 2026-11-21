package com.penny.wallet.core.money

import com.penny.wallet.core.money.Formatters.MoneyFormat
import com.penny.wallet.data.prefs.NumberFormatStyle
import com.penny.wallet.data.prefs.SymbolPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormattersTest {
    private val de = Locale.GERMANY
    private val it = Locale.ITALY
    private val us = Locale.US
    private fun f(code: String, locale: Locale, cents: Boolean = true) = MoneyFormat(code, cents, locale = locale)

    @Test fun euroInGermanAndItalian() {
        assertEquals("1.234,56 €", Formatters.money(123_456, f("EUR", de)))
        assertEquals("1.234,56 €", Formatters.money(123_456, f("EUR", it)))
        assertEquals("0,05 €", Formatters.money(5, f("EUR", de)))
        assertEquals("−12,00 €", Formatters.money(-1200, f("EUR", it)))
        assertEquals("+3,00 €", Formatters.money(300, f("EUR", de), signed = true))
    }

    @Test fun englishPutsSymbolFirst() {
        assertEquals("$1,234,567.89", Formatters.money(123_456_789, f("USD", us)))
        assertEquals("£12.50", Formatters.money(1250, f("GBP", Locale.UK)))
        assertEquals("€1,234.56", Formatters.money(123_456, f("EUR", Locale.UK)))
    }

    @Test fun letterSymbolsGetASpace() {
        assertEquals("CHF 12.50", Formatters.money(1250, MoneyFormat("CHF", style = NumberFormatStyle.US, symbolPosition = SymbolPosition.BEFORE, locale = us)))
    }

    @Test fun zeroAndThreeDecimalCurrencies() {
        assertEquals(0, Formatters.fractionDigits("JPY"))
        assertEquals(3, Formatters.fractionDigits("BHD"))
        assertTrue(Formatters.money(1234, f("JPY", us)).contains("1,234"))
        assertTrue(!Formatters.money(1234, f("JPY", us)).contains("."))
        assertEquals(1_234L, Formatters.parseCents("1.234", 0))
        assertEquals(1_234L, Formatters.parseCents("1,234", 3))
    }

    @Test fun overridesBeatTheLocale() {
        val eu = MoneyFormat("EUR", style = NumberFormatStyle.EU, symbolPosition = SymbolPosition.AFTER, locale = us)
        assertEquals("1.234,56 €", Formatters.money(123_456, eu))
        val usStyle = MoneyFormat("EUR", style = NumberFormatStyle.US, symbolPosition = SymbolPosition.BEFORE, locale = de)
        assertEquals("€1,234.56", Formatters.money(123_456, usStyle))
    }

    @Test fun hiddenCentsRound() {
        assertEquals("13 €", Formatters.money(1260, f("EUR", de, cents = false)))
        assertEquals("12,60 €", Formatters.money(1260, f("EUR", de, cents = false), forceCents = true))
    }

    @Test fun parse() {
        assertEquals(123_456L, Formatters.parseCents("1.234,56"))
        assertEquals(123_456L, Formatters.parseCents("1,234.56"))
        assertEquals(123_450L, Formatters.parseCents("1234.5"))
        assertEquals(1_200L, Formatters.parseCents("12"))
        assertEquals(1_200L, Formatters.parseCents("€ 12"))
        assertEquals(-500L, Formatters.parseCents("-5"))
        assertEquals(null, Formatters.parseCents("abc"))
    }

    @Test fun suggestedCurrencyFollowsRegion() {
        assertEquals("EUR", Formatters.suggestedCurrency(de))
        assertEquals("USD", Formatters.suggestedCurrency(us))
        assertEquals("EUR", Formatters.suggestedCurrency(Locale.ENGLISH))
    }

    @Test fun percentIsLocalized() {
        assertEquals("25%", Formatters.percent(0.25f, us))
        assertTrue(Formatters.percent(0.25f, de).startsWith("25"))
        assertTrue(Formatters.percent(0.001f, us).startsWith("<"))
    }
}
