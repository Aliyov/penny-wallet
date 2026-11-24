package com.penny.wallet.feature

import com.penny.wallet.data.db.FIXED_TODAY
import com.penny.wallet.data.db.LargeData
import com.penny.wallet.data.db.testDb
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.feature.expenses.Filters
import com.penny.wallet.feature.expenses.buildExpensesState
import com.penny.wallet.feature.home.buildHomeState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 20 000 entries, the size of a heavy user after several years. Limits are generous for slow CI runners;
 * they catch accidental O(n²) work, not small regressions. Every state is recomputed on each data change.
 */
@RunWith(RobolectricTestRunner::class)
class PerformanceTest {
    private val data = LargeData()
    private val prefs = UserPrefs(monthStartDay = 27, currencyCode = "EUR")

    private fun bestOf(runs: Int = 5, block: () -> Unit): Long {
        repeat(2) { block() }
        return (1..runs).minOf { val t = System.nanoTime(); block(); (System.nanoTime() - t) / 1_000_000 }
    }

    private fun assertWithin(limitMs: Long, what: String, block: () -> Unit) {
        val ms = bestOf(block = block)
        println("perf $what: $ms ms")
        assertTrue("$what took $ms ms (limit $limitMs)", ms <= limitMs)
    }

    @Test fun summary() = assertWithin(150, "summary") {
        MoneyCalculator.summary(data.accounts, data.goals, data.transactions)
    }

    @Test fun homeState() {
        val s = buildHomeState(data.accounts, data.categories, data.goals, data.transactions, data.recurring, prefs,
            hasSample = false, today = FIXED_TODAY, nowMillis = 0)
        assertEquals(5, s.accounts.size)
        assertWithin(400, "home state") {
            buildHomeState(data.accounts, data.categories, data.goals, data.transactions, data.recurring, prefs,
                hasSample = false, today = FIXED_TODAY, nowMillis = 0)
        }
    }

    @Test fun expensesSearch() {
        val f = Filters(month = null, category = null, query = "café")
        val s = buildExpensesState(data.transactions, data.categories, data.accounts, prefs, f, "Uncategorised", FIXED_TODAY)
        val rows = s.days.flatMap { it.rows }
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.tx.note == "Café Müller" })
        val byName = buildExpensesState(data.transactions, data.categories, data.accounts, prefs, f.copy(query = "category 1"), "Uncategorised", FIXED_TODAY)
        assertTrue(byName.days.isNotEmpty())
        assertWithin(400, "expenses search") {
            buildExpensesState(data.transactions, data.categories, data.accounts, prefs, f, "Uncategorised", FIXED_TODAY)
        }
        assertWithin(400, "expenses by category name") {
            buildExpensesState(data.transactions, data.categories, data.accounts, prefs, f.copy(query = "category 1"), "Uncategorised", FIXED_TODAY)
        }
    }

    @Test fun databaseLoad() = runBlocking {
        val db = testDb()
        data.insertInto(db)
        assertEquals(20_000, db.transactions().getAll().size)
        assertWithin(3_000, "load all transactions") { runBlocking { db.transactions().getAll() } }
        db.close()
    }
}
