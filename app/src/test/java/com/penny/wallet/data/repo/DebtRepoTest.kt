package com.penny.wallet.data.repo

import com.penny.wallet.data.db.FIXED_TODAY
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.db.testDb
import com.penny.wallet.domain.MoneyCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebtRepoTest {
    private val db = testDb()
    private val repo = DebtRepo(db, {}) { id, arg -> "label:$arg" }

    init { runBlocking { db.accounts().upsert(Account("bank", "Bank", "bank", sortOrder = 0, createdAt = 0)) } }

    @After fun close() = db.close()

    private fun reason(block: suspend () -> Unit) =
        (runCatching { runBlocking { block() } }.exceptionOrNull() as InvalidInputException).reason

    @Test fun partialPaymentsThenSettledByTheLastOne() = runBlocking {
        val d = repo.add(" Anna ", DebtDirection.I_OWE, 100_00, FIXED_TODAY.minusDays(10))
        assertEquals("Anna", d.person)
        repo.addPayment(d.id, 60_00, FIXED_TODAY)
        assertNull(repo.get(d.id)!!.settledOn)
        repo.addPayment(d.id, 40_00, FIXED_TODAY)
        assertEquals(FIXED_TODAY, repo.get(d.id)!!.settledOn)
    }

    @Test fun overpayingIsRefused() = runBlocking {
        val d = repo.add("Anna", DebtDirection.I_OWE, 10_00, FIXED_TODAY)
        assertEquals(InvalidInputException.Reason.DEBT_OVERPAID, reason { repo.addPayment(d.id, 10_01, FIXED_TODAY) })
        assertTrue(repo.payments().isEmpty())
    }

    @Test fun withoutRecordingBalancesNeverChange() = runBlocking {
        val d = repo.add("Marco", DebtDirection.OWED_TO_ME, 45_50, FIXED_TODAY)
        repo.addPayment(d.id, 45_50, FIXED_TODAY)
        assertTrue(db.transactions().getAll().isEmpty())
    }

    @Test fun recordedRepaymentsMoveTheAccountTheRightWay() = runBlocking {
        val owe = repo.add("Anna", DebtDirection.I_OWE, 100_00, FIXED_TODAY)
        val owed = repo.add("Marco", DebtDirection.OWED_TO_ME, 30_00, FIXED_TODAY)
        repo.addPayment(owe.id, 20_00, FIXED_TODAY, recordInAccount = "bank")
        repo.addPayment(owed.id, 30_00, FIXED_TODAY, recordInAccount = "bank")
        val txs = db.transactions().getAll()
        assertEquals(setOf(TxType.WITHDRAWAL, TxType.INCOME), txs.map { it.type }.toSet())
        assertEquals(10_00L, MoneyCalculator.accountBalance("bank", txs))
        assertEquals(0L, MoneyCalculator.monthSpent(txs, MoneyCalculator.periodOf(FIXED_TODAY, 1))) // paying back isn't spending
        assertTrue(repo.payments().all { it.transactionId != null })
        assertEquals(setOf("label:Anna", "label:Marco"), txs.map { it.note }.toSet())
    }

    @Test fun deletingAPaymentRemovesItsTransactionAndReopensTheDebt() = runBlocking {
        val d = repo.add("Anna", DebtDirection.I_OWE, 20_00, FIXED_TODAY)
        val p = repo.addPayment(d.id, 20_00, FIXED_TODAY, recordInAccount = "bank")
        repo.deletePayment(p)
        assertTrue(db.transactions().getAll().isEmpty())
        assertNull(repo.get(d.id)!!.settledOn)
    }

    @Test fun deleteAndUndoBringEverythingBack() = runBlocking {
        val d = repo.add("Anna", DebtDirection.I_OWE, 50_00, FIXED_TODAY)
        repo.addPayment(d.id, 10_00, FIXED_TODAY, recordInAccount = "bank")
        repo.addPayment(d.id, 5_00, FIXED_TODAY)
        val gone = repo.delete(d.id)!!
        assertTrue(repo.all().isEmpty() && repo.payments().isEmpty() && db.transactions().getAll().isEmpty())
        repo.restore(gone)
        assertEquals(listOf(d), repo.all())
        assertEquals(2, repo.payments().size)
        assertEquals(1, db.transactions().getAll().size)
    }

    @Test fun editsCantGoBelowWhatWasPaid() = runBlocking {
        val d = repo.add("Anna", DebtDirection.I_OWE, 50_00, FIXED_TODAY)
        repo.addPayment(d.id, 30_00, FIXED_TODAY)
        assertEquals(InvalidInputException.Reason.DEBT_BELOW_PAID, reason { repo.update(d.copy(amountCents = 20_00)) })
        repo.update(d.copy(amountCents = 30_00))
        assertNotNull(repo.get(d.id)!!.settledOn)
        repo.update(d.copy(amountCents = 60_00))
        assertNull(repo.get(d.id)!!.settledOn)
    }

    @Test fun reminderNeedsADueDateAndNamesAreRequired() = runBlocking {
        assertEquals(false, repo.add("Anna", DebtDirection.I_OWE, 1_00, FIXED_TODAY, dueDate = null, remind = true).remind)
        assertEquals(InvalidInputException.Reason.NAME_REQUIRED, reason { repo.add("  ", DebtDirection.I_OWE, 1_00, FIXED_TODAY) })
        assertEquals(InvalidInputException.Reason.AMOUNT_NOT_POSITIVE, reason { repo.add("Anna", DebtDirection.I_OWE, 0, FIXED_TODAY) })
    }

    @Test fun settleByHandAndReopen() = runBlocking {
        val d = repo.add("Anna", DebtDirection.OWED_TO_ME, 50_00, FIXED_TODAY)
        repo.setSettled(d.id, true, FIXED_TODAY)
        assertEquals(FIXED_TODAY, repo.get(d.id)!!.settledOn)
        repo.setSettled(d.id, false)
        assertNull(repo.get(d.id)!!.settledOn)
    }

    @Test fun handSettledDebtStaysSettledOnEditAndPaymentRemoval() = runBlocking {
        val d = repo.add("Anna", DebtDirection.OWED_TO_ME, 40_00, FIXED_TODAY)
        val p = repo.addPayment(d.id, 10_00, FIXED_TODAY)
        repo.setSettled(d.id, true, FIXED_TODAY)
        repo.update(repo.get(d.id)!!.copy(note = "forgiven"))
        assertEquals(FIXED_TODAY, repo.get(d.id)!!.settledOn)
        repo.deletePayment(p)
        assertEquals(FIXED_TODAY, repo.get(d.id)!!.settledOn)
    }

    @Test fun removedPaymentComesBackWithItsTransactionOnUndo() = runBlocking {
        val d = repo.add("Anna", DebtDirection.OWED_TO_ME, 40_00, FIXED_TODAY)
        val p = repo.addPayment(d.id, 40_00, FIXED_TODAY, recordInAccount = "bank")
        val gone = repo.deletePayment(p)!!
        assertNull(repo.get(d.id)!!.settledOn)
        assertTrue(db.transactions().getAll().isEmpty())
        repo.restorePayment(gone)
        assertEquals(listOf(p), repo.payments())
        assertEquals(1, db.transactions().getAll().size)
        assertEquals(FIXED_TODAY, repo.get(d.id)!!.settledOn)
    }

    @Test fun directionIsLockedOnceThereAreRepayments() = runBlocking {
        val d = repo.add("Anna", DebtDirection.OWED_TO_ME, 40_00, FIXED_TODAY)
        repo.update(d.copy(direction = DebtDirection.I_OWE)) // no payments yet: fine
        repo.addPayment(d.id, 5_00, FIXED_TODAY, recordInAccount = "bank")
        assertEquals(InvalidInputException.Reason.DEBT_HAS_PAYMENTS, reason { repo.update(repo.get(d.id)!!.copy(direction = DebtDirection.OWED_TO_ME)) })
    }

    @Test fun raisingTheAmountOfAFullyPaidDebtReopensIt() = runBlocking {
        val d = repo.add("Anna", DebtDirection.I_OWE, 20_00, FIXED_TODAY)
        repo.addPayment(d.id, 20_00, FIXED_TODAY)
        repo.update(repo.get(d.id)!!.copy(amountCents = 25_00))
        assertNull(repo.get(d.id)!!.settledOn)
    }
}
