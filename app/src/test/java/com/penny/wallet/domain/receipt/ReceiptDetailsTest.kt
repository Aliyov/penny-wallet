package com.penny.wallet.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReceiptDetailsTest {
    private val today = LocalDate.of(2026, 11, 24)

    @Test fun shopNameFromFirstHeaderRow() {
        val text = """
            ESSELUNGA SPA
            Via Roma 12
            P.IVA 01234567890
            PANE 1,20
            TOTALE 1,20
            21/11/2026 18:04
        """.trimIndent()
        val r = ReceiptParser.parseText(text, today)
        assertEquals("Esselunga SPA", r.merchant)
        assertEquals(LocalDate.of(2026, 11, 21), r.date)
        assertEquals(120L, r.totalCents)
    }

    @Test fun shortCapsNameStays() = assertEquals("LIDL", ReceiptDetails.merchant(listOf("LIDL", "Hauptstr. 5")))

    @Test fun skipsTitleAndContactRows() {
        val rows = listOf("Kassenbon", "www.example.de", "Bäckerei Müller", "Brot 2,50")
        assertEquals("Bäckerei Müller", ReceiptDetails.merchant(rows))
    }

    @Test fun thankYouLineIsNotAShop() =
        assertNull(ReceiptDetails.merchant(listOf("Vielen Dank für Ihren Einkauf", "Grazie e arrivederci")))

    @Test fun noNameWhenHeaderIsOnlyNumbersOrTotals() {
        assertNull(ReceiptDetails.merchant(listOf("12345 678", "TOTAL 5,00")))
    }

    @Test fun dateFormats() {
        assertEquals(LocalDate.of(2026, 11, 3), ReceiptDetails.date(listOf("03.11.2026"), today))
        assertEquals(LocalDate.of(2026, 11, 3), ReceiptDetails.date(listOf("Datum 03.11.26 12:00"), today))
        assertEquals(LocalDate.of(2026, 10, 30), ReceiptDetails.date(listOf("2026-10-30"), today))
        assertEquals(LocalDate.of(2026, 11, 20), ReceiptDetails.date(listOf("11/20/2026"), today))
    }

    @Test fun ignoresFutureOldAndImpossibleDates() {
        assertNull(ReceiptDetails.date(listOf("25.12.2026"), today))
        assertNull(ReceiptDetails.date(listOf("01.01.2020"), today))
        assertNull(ReceiptDetails.date(listOf("31.02.2026"), today))
        assertNull(ReceiptDetails.date(listOf("PANE 1,20"), today))
    }

    @Test fun minorUnitRescaleKeepsDetails() {
        val r = ReceiptResult(1_200, listOf(1_200), true, merchant = "Shop", date = today).inMinorUnits(0)
        assertEquals(12L, r.totalCents)
        assertEquals("Shop", r.merchant)
        assertEquals(today, r.date)
    }
}
