package com.penny.wallet.domain

import com.penny.wallet.data.db.LargeData
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    private val accounts = listOf(Account("bank", "Main Bank", "bank", sortOrder = 0, createdAt = 0), Account("cash", "Cash", "cash", sortOrder = 1, createdAt = 0))
    private val cats = listOf(Category("food", "Eating out", "other", 0, sortOrder = 0), Category("car", "Transport", "other", 0, sortOrder = 1))
    private val goals = listOf(Goal("trip", "Trip to Rome", "plane", 0, 100_000, sortOrder = 0, createdAt = 0))
    private val txs = listOf(
        t(TxType.EXPENSE, 4_50, "bank", date = "2026-11-20", cat = "food").copy(note = "Café Müller"),
        t(TxType.EXPENSE, 60_00, "bank", date = "2026-10-02", cat = "car").copy(note = "Fuel"),
        t(TxType.INCOME, 2200_00, "bank", date = "2026-11-01").copy(note = "Salary November"),
        t(TxType.TRANSFER, 100_00, "bank", date = "2025-12-24", to = "cash"),
        t(TxType.SAVE, 50_00, "bank", date = "2026-03-01", goal = "trip"),
        t(TxType.EXPENSE, 12_00, "cash", date = "2024-02-29", cat = "food"),
    )

    private fun run(f: SearchFilter) = Search.run(txs, accounts, cats, goals, f)
    private fun ids(f: SearchFilter) = run(f).rows.map { it.amountCents }

    @Test fun emptySearchListsEverythingNewestFirst() {
        assertEquals(listOf(4_50L, 2200_00L, 60_00L, 50_00L, 100_00L, 12_00L), ids(SearchFilter()))
    }

    @Test fun matchesNoteCategoryAccountAndGoalIgnoringCaseAndAccents() {
        assertEquals(listOf(4_50L), ids(SearchFilter("cafe muller")))
        assertEquals(listOf(4_50L, 12_00L), ids(SearchFilter("EATING")))
        assertEquals(listOf(100_00L, 12_00L), ids(SearchFilter("cash")))
        assertEquals(listOf(50_00L), ids(SearchFilter("rome")))
        assertEquals(emptyList<Long>(), ids(SearchFilter("cafe fuel")))
    }

    @Test fun periodAmountAndKindFilters() {
        assertEquals(listOf(4_50L, 2200_00L), ids(SearchFilter(from = d("2026-11-01"), to = d("2026-11-30"))))
        assertEquals(listOf(60_00L, 50_00L, 100_00L), ids(SearchFilter(minCents = 50_00, maxCents = 100_00)))
        assertEquals(listOf(4_50L, 60_00L, 12_00L), ids(SearchFilter(kind = SearchKind.EXPENSES)))
        assertEquals(listOf(12_00L), ids(SearchFilter("eating", to = d("2025-01-01"), kind = SearchKind.EXPENSES)))
    }

    @Test fun totalsCountSpentAndReceivedSeparately() {
        val r = run(SearchFilter())
        assertEquals(76_50L, r.spent)
        assertEquals(2200_00L, r.received)
    }

    @Test fun twentyThousandEntriesStayFast() {
        val data = LargeData()
        val f = SearchFilter("cafe", minCents = 1_00, kind = SearchKind.EXPENSES)
        repeat(2) { Search.run(data.transactions, data.accounts, data.categories, data.goals, f) }
        val start = System.nanoTime()
        val r = Search.run(data.transactions, data.accounts, data.categories, data.goals, f)
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("search took $ms ms", ms < 400)
        assertTrue(r.rows.isNotEmpty() && r.rows.all { it.note == "Café Müller" })
        assertTrue(r.rows.zipWithNext().all { (a, b) -> !a.date.isBefore(b.date) })
    }

    @Test fun foldHandlesLettersWithoutCombiningMarks() {
        assertEquals("strasse", Search.fold("Straße"))
        assertEquals("lodz", Search.fold("Łódź"))
        assertEquals("aeroe sondergard", Search.fold("Ærøe Søndergård"))
    }

    @Test fun rowsWithoutANoteAreFoundByTheirKind() {
        val r = Search.run(txs, accounts, cats, goals, SearchFilter("transfer"), mapOf(TxType.TRANSFER to "Transfer"))
        assertEquals(listOf(100_00L), r.rows.map { it.amountCents })
    }
}
