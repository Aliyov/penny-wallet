package com.penny.wallet.feature

import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.domain.d
import com.penny.wallet.domain.t
import com.penny.wallet.feature.expenses.Filters
import com.penny.wallet.feature.expenses.buildExpensesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpensesBudgetStateTest {
    private val accounts = listOf(Account("a", "Bank", "bank", sortOrder = 0, createdAt = 0))
    private val cats = listOf(
        Category("food", "Food", "other", 0, sortOrder = 0, monthlyBudgetCents = 300_00),
        Category("fun", "Fun", "other", 0, sortOrder = 1),
        Category("gym", "Gym", "other", 0, sortOrder = 2, monthlyBudgetCents = 40_00),
        Category("old", "Old", "other", 0, archived = true, sortOrder = 3, monthlyBudgetCents = 10_00),
    )
    private val txs = listOf(
        t(TxType.EXPENSE, 120_00, "a", date = "2026-11-05", cat = "food"),
        t(TxType.EXPENSE, 80_00, "a", date = "2026-11-06", cat = "fun"),
    )

    private fun state(month: java.time.YearMonth? = null) =
        buildExpensesState(txs, cats, accounts, UserPrefs(), Filters(month, null, ""), "-", d("2026-11-10"))

    @Test fun budgetedSliceCarriesItsStatus() {
        val s = state()
        val food = s.slices.single { it.categoryId == "food" }
        assertEquals(180_00L, food.budget!!.left)
        assertNull(s.slices.single { it.categoryId == "fun" }.budget)
    }

    @Test fun unspentBudgetsAreListedButNotCharted() {
        val s = state()
        assertEquals(listOf("gym"), s.unspentBudgets.map { it.categoryId })
        assertEquals(40_00L, s.unspentBudgets.single().budget!!.left)
        assertEquals(setOf("food", "fun"), s.slices.map { it.categoryId }.toSet())
        assertEquals(200_00L, s.total)
    }

    @Test fun pastMonthsShowNoBudgets() {
        val past = listOf(t(TxType.EXPENSE, 50_00, "a", date = "2026-10-05", cat = "food"))
        val s = buildExpensesState(txs + past, cats, accounts, UserPrefs(), Filters(java.time.YearMonth.of(2026, 10), null, ""), "-", d("2026-11-10"))
        assertEquals(listOf("food"), s.slices.map { it.categoryId })
        assertNull(s.slices.single().budget)
        assertEquals(emptyList<String?>(), s.unspentBudgets.map { it.categoryId })
    }
}
