package com.penny.wallet.domain.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CsvParsingTest {
    @Test fun amountsInEveryCommonNotation() {
        val cases = mapOf(
            "12.50" to 1250L, "12,50" to 1250L, "1.234,56" to 123456L, "1,234.56" to 123456L, "1 234,56" to 123456L,
            "1'234.56" to 123456L, "-12,5" to -1250L, "(12.50)" to -1250L, "12.50-" to -1250L, "+7" to 700L, "€ 9,99" to 999L,
            "9.99 EUR" to 999L, "−3,00" to -300L, "1.234" to 123400L, "1,234" to 123400L, "1.234.567,8" to 123456780L, "0,01" to 1L,
            ".5" to 50L, "12" to 1200L, "12.345,678" to 1234568L,
        )
        cases.forEach { (text, cents) -> assertEquals(text, cents, CsvAmount.parse(text, 2)) }
    }

    @Test fun zeroAndThreeDecimalCurrencies() {
        assertEquals(1234L, CsvAmount.parse("1.234", 0))
        assertEquals(1234L, CsvAmount.parse("1,234", 3)) // three decimals: 1.234 KWD
        assertEquals(1235L, CsvAmount.parse("1234.5", 0))
    }

    @Test fun notNumbers() {
        listOf("", "abc", "-", "1-2", "((3))", "1.2.3,4.5", "12,34,56.7.8", "--").forEach { assertNull(it, CsvAmount.parse(it, 2)) }
        assertNull(CsvAmount.parse("9".repeat(40), 2))
    }

    @Test fun dateFormatsAreDetectedPerFile() {
        assertEquals("yyyy-MM-dd", CsvDate.detect(listOf("2026-11-24", "2026-01-02 13:05:00", "2026-03-04T08:00")))
        assertEquals("dd.MM.yyyy", CsvDate.detect(listOf("24.11.2026", "01.02.2026")))
        assertEquals("dd/MM/yyyy", CsvDate.detect(listOf("03/04/2026", "24/11/2026")))
        assertEquals("MM/dd/yyyy", CsvDate.detect(listOf("03/04/2026", "11/24/2026")))
        assertEquals("dd.MM.yy", CsvDate.detect(listOf("24.11.26")))
        assertEquals(LocalDate.of(2026, 4, 3), CsvDate.parse("03/04/2026", "dd/MM/yyyy"))
        assertNull(CsvDate.detect(listOf("31.02.2026")))
        assertNull(CsvDate.detect(listOf("hello")))
    }

    @Test fun readerHandlesQuotesNewlinesAndDelimiters() {
        assertEquals(listOf(listOf("a", "b,c", "say \"hi\""), listOf("1", "2", "3")), CsvReader.read("a,\"b,c\",\"say \"\"hi\"\"\"\r\n1,2,3\r\n"))
        assertEquals(listOf(listOf("a", "line1\nline2"), listOf("x", "y")), CsvReader.read("a;\"line1\nline2\"\nx;y"))
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), CsvReader.read("\uFEFFa\tb\n1\t2\n\n"))
        assertEquals(listOf(listOf("only")), CsvReader.read("only"))
        assertEquals(emptyList<List<String>>(), CsvReader.read(""))
    }
}
