package com.penny.wallet.data.db

import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.DebtPayment
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebtLinksTest {
    private val db = testDb().withSampleData()

    @After fun close() = db.close()

    private suspend fun debtWithPayment(): Pair<Debt, DebtPayment> {
        val tx = db.transactions().getAll().first()
        val d = Debt("d1", "Anna", DebtDirection.I_OWE, 100_00, FIXED_TODAY, createdAt = 1)
        val p = DebtPayment("p1", d.id, 40_00, FIXED_TODAY, transactionId = tx.id, createdAt = 2)
        db.debts().upsert(d); db.debts().upsertPayment(p)
        return d to p
    }

    @Test fun deletingTheLinkedTransactionKeepsThePaymentUnlinked() = runBlocking {
        val (_, p) = debtWithPayment()
        db.transactions().deleteIds(listOf(p.transactionId!!))
        val left = db.debts().getAllPayments().single()
        assertNull(left.transactionId)
        assertEquals(40_00L, left.amountCents)
    }

    @Test fun deletingADebtDeletesItsPayments() = runBlocking {
        val (d, _) = debtWithPayment()
        db.debts().delete(d.id)
        assertTrue(db.debts().getAllPayments().isEmpty())
    }

    @Test fun updatingTheLinkedTransactionKeepsTheLink() = runBlocking {
        val (_, p) = debtWithPayment()
        val tx = db.transactions().getAll().single { it.id == p.transactionId }
        db.transactions().upsert(tx.copy(note = "edited"))
        assertEquals(tx.id, db.debts().getAllPayments().single().transactionId)
    }

    @Test fun removingSampleDataUnlinksPaymentsToSampleTransactions() = runBlocking {
        debtWithPayment()
        db.debts().upsert(Debt("sample-d", "Sample", DebtDirection.OWED_TO_ME, 10_00, FIXED_TODAY, createdAt = 3))
        SampleData.remove(db)
        assertEquals(listOf("d1"), db.debts().getAll().map { it.id })
        assertNull(db.debts().getAllPayments().single().transactionId)
    }
}
