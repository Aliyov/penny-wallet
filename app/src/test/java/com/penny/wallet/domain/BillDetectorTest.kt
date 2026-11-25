package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BillDetectorTest {
    private val today = d("2026-11-20")

    private fun exp(date: String, amount: Long = 12_99, note: String? = "Netflix", cat: String? = "subs") =
        t(TxType.EXPENSE, amount, "bank", date = date, cat = cat).copy(note = note)

    private fun detect(vararg txs: Transaction, dismissed: Set<String> = emptySet()) =
        BillDetector.suggestions(txs.toList(), emptyList(), dismissed, today)

    @Test fun threeMonthlyPaymentsAreABill() {
        val s = detect(exp("2026-08-14"), exp("2026-09-14"), exp("2026-10-14"), exp("2026-11-14")).single()
        assertEquals("Netflix", s.name)
        assertEquals(14, s.dayOfMonth)
        assertEquals(4, s.occurrences)
        assertEquals(12_99L, s.amountCents)
        assertEquals("subs", s.categoryId)
        assertEquals("bank", s.accountId)
    }

    @Test fun twoPaymentsAreNotEnough() = assertTrue(detect(exp("2026-10-14"), exp("2026-11-14")).isEmpty())

    @Test fun weeklyOrIrregularIsNotMonthly() {
        assertTrue(detect(exp("2026-10-30"), exp("2026-11-06"), exp("2026-11-13"), exp("2026-11-20")).isEmpty())
        assertTrue(detect(exp("2026-07-02"), exp("2026-09-01"), exp("2026-11-02")).isEmpty())
    }

    @Test fun monthEndsAndFebruaryStillCount() {
        val s = detect(exp("2026-01-31"), exp("2026-02-28"), exp("2026-03-31"), today = d("2026-04-10")).single()
        assertEquals(31, s.dayOfMonth)
    }

    @Test fun leapYearFebruary() = assertEquals(1, detect(exp("2028-01-29"), exp("2028-02-29"), exp("2028-03-29"), today = d("2028-04-01")).size)

    @Test fun priceChangeWithinTenPercentKeepsTheLatestAmount() {
        val s = detect(exp("2026-08-14", 12_99), exp("2026-09-14", 12_99), exp("2026-10-14", 13_99), exp("2026-11-14", 13_99)).single()
        assertEquals(13_99L, s.amountCents)
    }

    @Test fun wildlyDifferentAmountsAreNotABill() {
        assertTrue(detect(exp("2026-09-01", 20_00, "Supermarket", "food"), exp("2026-10-01", 85_00, "Supermarket", "food"), exp("2026-11-01", 43_00, "Supermarket", "food")).isEmpty())
    }

    @Test fun smallBillsGetAFewCentsOfLeeway() = assertEquals(1, detect(exp("2026-09-05", 2_99), exp("2026-10-05", 3_49), exp("2026-11-05", 2_99)).size)

    @Test fun notesMatchIgnoringCaseAndSpaces() = assertEquals(1, detect(exp("2026-09-14", note = "netflix"), exp("2026-10-14", note = " NETFLIX "), exp("2026-11-14", note = "Netflix")).size)

    @Test fun sameNoteInAnotherCategoryIsSeparate() =
        assertTrue(detect(exp("2026-09-14", cat = "a"), exp("2026-10-14", cat = "b"), exp("2026-11-14", cat = "a")).isEmpty())

    @Test fun withoutNoteOrCategoryNothingIsGuessed() {
        assertTrue(detect(exp("2026-09-14", note = null), exp("2026-10-14", note = null), exp("2026-11-14", note = null)).isEmpty())
        assertTrue(detect(exp("2026-09-14", note = "  "), exp("2026-10-14", note = " "), exp("2026-11-14", note = "")).isEmpty())
        assertTrue(detect(exp("2026-09-14", cat = null), exp("2026-10-14", cat = null), exp("2026-11-14", cat = null)).isEmpty())
    }

    @Test fun stoppedBillsAreNotSuggested() = assertTrue(detect(exp("2026-07-01"), exp("2026-08-01"), exp("2026-09-01")).isEmpty())

    @Test fun onlyTheNewestRunCounts() {
        // Weekly for a while, then monthly for three months.
        val s = detect(exp("2026-06-01"), exp("2026-06-08"), exp("2026-06-15"), exp("2026-09-01"), exp("2026-10-01"), exp("2026-11-01")).single()
        assertEquals(3, s.occurrences)
    }

    @Test fun existingBillsAndDismissalsAreRespected() {
        val txs = arrayOf(exp("2026-09-14"), exp("2026-10-14"), exp("2026-11-14"))
        val rule = rule().copy(name = " netflix", categoryId = "subs")
        assertTrue(BillDetector.suggestions(txs.toList(), listOf(rule), emptySet(), today).isEmpty())
        assertTrue(detect(*txs, dismissed = setOf(BillDetector.key("Netflix", "subs"))).isEmpty())
        assertEquals(1, BillDetector.suggestions(txs.toList(), listOf(rule.copy(categoryId = "other")), emptySet(), today).size)
    }

    @Test fun paymentsAlreadyLinkedToABillSplitsIncomeAndFutureAreIgnored() {
        assertTrue(detect(exp("2026-09-14").copy(recurringId = "r"), exp("2026-10-14").copy(recurringId = "r"), exp("2026-11-14").copy(recurringId = "r")).isEmpty())
        assertTrue(detect(exp("2026-09-14").copy(splitId = "s"), exp("2026-10-14").copy(splitId = "s"), exp("2026-11-14").copy(splitId = "s")).isEmpty())
        assertTrue(detect(exp("2026-09-14").copy(type = TxType.INCOME), exp("2026-10-14").copy(type = TxType.INCOME), exp("2026-11-14").copy(type = TxType.INCOME)).isEmpty())
        assertTrue(detect(exp("2026-10-14"), exp("2026-11-14"), exp("2026-12-14")).isEmpty())
    }

    @Test fun newestSuggestionFirst() {
        val s = detect(
            exp("2026-08-03", note = "Gym", cat = "fit"), exp("2026-09-03", note = "Gym", cat = "fit"), exp("2026-10-03", note = "Gym", cat = "fit"),
            exp("2026-09-14"), exp("2026-10-14"), exp("2026-11-14"),
        )
        assertEquals(listOf("Netflix"), s.map { it.name })
        val both = detect(
            exp("2026-09-03", note = "Gym", cat = "fit"), exp("2026-10-03", note = "Gym", cat = "fit"), exp("2026-11-03", note = "Gym", cat = "fit"),
            exp("2026-09-14"), exp("2026-10-14"), exp("2026-11-14"),
        )
        assertEquals(listOf("Netflix", "Gym"), both.map { it.name })
    }

    @Test fun tiedDaysUseTheNewestPayment() {
        val s = detect(exp("2025-12-30"), exp("2026-02-02"), exp("2026-03-01"), today = d("2026-03-10")).single()
        assertEquals(1, s.dayOfMonth)
    }

    @Test fun archivedAccountsAndCategoriesAreNotSuggested() {
        val txs = listOf(exp("2026-09-14"), exp("2026-10-14"), exp("2026-11-14"))
        assertTrue(BillDetector.suggestions(txs, emptyList(), emptySet(), today, activeAccounts = setOf("other")).isEmpty())
        assertTrue(BillDetector.suggestions(txs, emptyList(), emptySet(), today, activeCategories = setOf("other")).isEmpty())
        assertEquals(1, BillDetector.suggestions(txs, emptyList(), emptySet(), today, setOf("bank"), setOf("subs")).size)
    }

    @Test fun suggestionKnowsItsSourcePayments() {
        val txs = arrayOf(exp("2026-06-01"), exp("2026-09-14"), exp("2026-10-14"), exp("2026-11-14"))
        assertEquals(txs.drop(1).map { it.id }.toSet(), detect(*txs).single().transactionIds.toSet())
    }

    @Test fun randomLedgersNeverCrash() {
        val rnd = Random(3)
        val notes = listOf("Netflix", "netflix ", "Gym", "", null, "Rent", "Café")
        repeat(300) {
            val txs = List(rnd.nextInt(0, 60)) {
                exp(today.minusDays(rnd.nextLong(0, 400)).toString(), rnd.nextLong(1, 200_00), notes.random(rnd), listOf("a", "b", null).random(rnd))
            }
            BillDetector.suggestions(txs, emptyList(), emptySet(), today).forEach { s ->
                assertTrue(s.occurrences >= BillDetector.MIN_OCCURRENCES)
                assertTrue(s.dayOfMonth in 1..31)
            }
        }
    }

    private fun detect(vararg txs: Transaction, today: java.time.LocalDate) = BillDetector.suggestions(txs.toList(), emptyList(), emptySet(), today)
}
