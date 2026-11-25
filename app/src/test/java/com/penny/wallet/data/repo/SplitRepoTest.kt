package com.penny.wallet.data.repo

import com.penny.wallet.data.db.FIXED_TODAY
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.testDb
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.domain.SplitPart
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SplitRepoTest {
    private val db = testDb()
    private val repo = TransactionRepo(db, {}) { "" }
    private val parts = listOf(SplitPart("food", 30_00), SplitPart("home", 15_50), SplitPart("kids", 4_50))

    init { runBlocking { db.accounts().upsert(Account("bank", "Bank", "bank", sortOrder = 0, createdAt = 0)) } }

    @After fun close() = db.close()

    private fun add() = runBlocking { repo.addSplit(parts, "bank", FIXED_TODAY, "Supermarket") }

    @Test fun partsShareSplitAccountDateAndNote() {
        val rows = add()
        assertEquals(3, rows.size)
        assertEquals(1, rows.map { it.splitId }.distinct().size)
        assertTrue(rows.all { it.accountId == "bank" && it.date == FIXED_TODAY && it.note == "Supermarket" })
        assertEquals(listOf("food", "home", "kids"), runBlocking { repo.splitParts(rows[0].splitId!!) }.map { it.categoryId })
    }

    @Test fun totalsCountThePartsLikeOrdinaryExpenses() = runBlocking {
        add()
        val txs = repo.all()
        val period = MoneyCalculator.periodOf(FIXED_TODAY, 1)
        assertEquals(50_00L, MoneyCalculator.monthSpent(txs, period))
        assertEquals(-50_00L, MoneyCalculator.accountBalance("bank", txs))
        assertEquals(mapOf<String?, Long>("food" to 30_00, "home" to 15_50, "kids" to 4_50), MoneyCalculator.spentByCategory(txs, period))
    }

    @Test fun deletingOnePartDeletesTheSplitAndUndoBringsAllBack() = runBlocking {
        val rows = add()
        repo.delete(rows[1])
        assertTrue(repo.all().isEmpty())
        repo.restore(rows[1])
        assertEquals(rows.sortedBy { it.id }, repo.all().sortedBy { it.id })
    }

    @Test fun editKeepsIdsAndOrderByPosition() = runBlocking {
        val rows = add()
        val split = rows[0].splitId!!
        val two = repo.replaceSplit(split, listOf(SplitPart("food", 45_00), SplitPart("home", 5_00)), "bank", FIXED_TODAY, "Supermarket")
        assertEquals(rows.take(2).map { it.id to it.createdAt }, two.map { it.id to it.createdAt })
        assertEquals(setOf(rows[0].id, rows[1].id), repo.all().map { it.id }.toSet())
        val four = repo.replaceSplit(split, listOf(SplitPart("food", 20_00), SplitPart("home", 10_00), SplitPart("kids", 10_00), SplitPart("fun", 10_00)), "bank", FIXED_TODAY, null)
        assertEquals(rows.take(2).map { it.id }, four.take(2).map { it.id })
        assertEquals(4, repo.splitParts(split).size)
    }

    @Test fun undoAfterARestartBringsBackAnOrdinaryExpense() = runBlocking {
        val rows = add()
        repo.delete(rows[1])
        val afterRestart = TransactionRepo(db, {}) { "" }
        afterRestart.restore(rows[1])
        val back = repo.all().single()
        assertEquals(null, back.splitId)
        assertEquals(rows[1].amountCents, back.amountCents)
    }

    @Test fun replaceSwapsEveryPart() = runBlocking {
        val rows = add()
        val split = rows[0].splitId!!
        repo.replaceSplit(split, listOf(SplitPart("food", 40_00), SplitPart("fun", 10_00)), "bank", FIXED_TODAY.minusDays(1), null)
        val now = repo.splitParts(split)
        assertEquals(listOf(40_00L, 10_00L), now.map { it.amountCents })
        assertTrue(now.all { it.date == FIXED_TODAY.minusDays(1) && it.note == null })
        assertEquals(2, repo.all().size)
    }

    @Test fun invalidSplitsAreRejectedAndNothingIsSaved() = runBlocking {
        fun reason(p: List<SplitPart>) = runCatching { runBlocking { repo.addSplit(p, "bank", FIXED_TODAY, null) } }.exceptionOrNull() as InvalidInputException
        assertEquals(InvalidInputException.Reason.SPLIT_TOO_FEW, reason(listOf(SplitPart("a", 5_00))).reason)
        assertEquals(InvalidInputException.Reason.SPLIT_NO_CATEGORY, reason(listOf(SplitPart("a", 5_00), SplitPart(null, 1_00))).reason)
        assertEquals(InvalidInputException.Reason.AMOUNT_NOT_POSITIVE, reason(listOf(SplitPart("a", 5_00), SplitPart("b", 0))).reason)
        assertTrue(repo.all().isEmpty())
    }

    @Test fun ordinaryExpensesStillDeleteAndRestoreAlone() = runBlocking {
        add()
        val single = repo.addExpense(9_99, "bank", "food", FIXED_TODAY)
        repo.delete(single)
        assertEquals(3, repo.all().size)
        repo.restore(single)
        assertEquals(4, repo.all().size)
    }
}
