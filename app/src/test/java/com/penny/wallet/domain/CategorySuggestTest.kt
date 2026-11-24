package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class CategorySuggestTest {
    private val zone = ZoneOffset.UTC
    private var n = 0

    private fun exp(cat: String, at: LocalDateTime, note: String? = null) = Transaction(
        "x${n++}", TxType.EXPENSE, 100, "a", categoryId = cat, date = at.toLocalDate(), note = note,
        createdAt = at.toInstant(zone).toEpochMilli(),
    )

    private val monday = LocalDate.of(2026, 11, 23)

    @Test fun merchantMatchesNoteRegardlessOfCaseAndAccents() {
        val txs = listOf(
            exp("food", monday.atTime(10, 0), "Lidl"),
            exp("food", monday.atTime(11, 0), "LIDL Berlin"),
            exp("home", monday.atTime(12, 0), "Lidl"),
            exp("fun", monday.atTime(12, 0), "Cinema"),
        )
        assertEquals("food", CategorySuggest.forMerchant("lidl", txs))
        assertEquals("food", CategorySuggest.forMerchant("Lídl", txs))
    }

    @Test fun merchantNeedsAWholeWordMatch() {
        val txs = listOf(exp("food", monday.atTime(10, 0), "Lidlerhof"))
        assertNull(CategorySuggest.forMerchant("Lidl", txs))
        assertNull(CategorySuggest.forMerchant("Li", txs))
    }

    @Test fun habitByTimeOfDay() {
        val txs = (0 until 6).map { exp("coffee", monday.minusDays(it.toLong() * 7).atTime(8, 15)) } +
            (0 until 6).map { exp("dinner", monday.minusDays(it.toLong() * 7).atTime(20, 0)) }
        assertEquals("coffee", CategorySuggest.forTime(txs, monday.plusDays(7).atTime(8, 30), zone))
        assertEquals("dinner", CategorySuggest.forTime(txs, monday.plusDays(7).atTime(19, 30), zone))
    }

    @Test fun noHabitWithoutEnoughHistory() {
        val txs = (0 until 4).map { exp("coffee", monday.minusDays(it.toLong()).atTime(8, 0)) }
        assertNull(CategorySuggest.forTime(txs, monday.atTime(8, 0), zone))
    }

    @Test fun noHabitWhenNothingStandsOut() {
        val cats = listOf("a", "b", "c", "d", "e")
        val txs = (0 until 15).map { exp(cats[it % 5], monday.minusDays(it.toLong()).atTime(12, 0)) }
        assertNull(CategorySuggest.forTime(txs, monday.atTime(12, 0), zone))
    }

    @Test fun hoursWrapAroundMidnight() {
        val txs = (0 until 8).map { exp("late", monday.minusDays(it.toLong()).atTime(23, 30)) }
        assertEquals("late", CategorySuggest.forTime(txs, monday.atTime(0, 15), zone))
    }
}
