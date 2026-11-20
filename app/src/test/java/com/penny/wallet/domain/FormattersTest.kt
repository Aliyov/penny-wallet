package com.penny.wallet.domain

import com.penny.wallet.data.prefs.NumberFormatStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test fun euDefault() {
        assertEquals("1.234,56 €", Formatters.money(123_456))
        assertEquals("0,05 €", Formatters.money(5))
        assertEquals("−12,00 €", Formatters.money(-1200))
        assertEquals("+3,00 €", Formatters.money(300, signed = true))
    }

    @Test fun usAndNoCents() {
        val us = Formatters.MoneyFormat(style = NumberFormatStyle.US, symbolBefore = true)
        assertEquals("€1,234,567.89", Formatters.money(123_456_789, us))
        assertEquals("13 €", Formatters.money(1260, Formatters.MoneyFormat(showCents = false)))
    }

    @Test fun parse() {
        assertEquals(123_456L, Formatters.parseCents("1.234,56"))
        assertEquals(123_450L, Formatters.parseCents("1234.5"))
        assertEquals(1_200L, Formatters.parseCents("12"))
        assertEquals(-500L, Formatters.parseCents("-5"))
        assertEquals(null, Formatters.parseCents("abc"))
    }
}
