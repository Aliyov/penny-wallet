package com.penny.wallet.data.repo

import androidx.room.withTransaction
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.domain.Recurrence
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class RecurringRepo(private val db: PennyDatabase, private val listener: ChangeListener) {
    private val dao = db.recurring()

    fun observe(): Flow<List<RecurringExpense>> = dao.observeAll()
    suspend fun all() = dao.getAll()
    suspend fun get(id: String) = dao.get(id)

    suspend fun save(r: RecurringExpense) { dao.upsert(r); listener.onDataChanged() }
    suspend fun delete(r: RecurringExpense) { dao.delete(r); listener.onDataChanged() }
    suspend fun restore(r: RecurringExpense) = save(r)

    /**
     * Pays the occurrence due on [expectedDue] (defaults to the rule's current nextDueDate).
     * Idempotent: no-op if the rule has already moved past [expectedDue] (double tap, worker + receiver race)
     * or if an expense for (rule, date) already exists. The expense is dated [paidOn] (clamped to ≤ due when
     * paying early, = due for auto-pay catch-up). Returns null when nothing was inserted.
     */
    suspend fun pay(ruleId: String, expectedDue: LocalDate? = null, paidOn: LocalDate? = null): Payment? {
        val result = db.withTransaction {
            val r = dao.get(ruleId) ?: return@withTransaction null
            val due = r.nextDueDate
            if (expectedDue != null && expectedDue != due) return@withTransaction null
            val date = paidOn ?: due
            if (db.transactions().countForRecurring(r.id, date) > 0) {
                dao.upsert(r.copy(nextDueDate = Recurrence.advance(r)))
                return@withTransaction null
            }
            val t = Transaction(
                newId(), TxType.EXPENSE, r.amountCents, r.accountId, categoryId = r.categoryId,
                recurringId = r.id, date = date, note = r.name, createdAt = now(),
            )
            db.transactions().insert(t)
            dao.upsert(r.copy(nextDueDate = Recurrence.advance(r)))
            Payment(t, due)
        }
        listener.onDataChanged()
        return result
    }

    /** Undo of a payment: delete the expense and move nextDueDate back to the paid occurrence. */
    suspend fun undoPay(p: Payment) {
        db.withTransaction {
            db.transactions().delete(p.transaction)
            val r = p.transaction.recurringId?.let { dao.get(it) } ?: return@withTransaction
            dao.upsert(r.copy(nextDueDate = p.dueDate))
        }
        listener.onDataChanged()
    }

    /** Undo from a notification, where only ids survive. */
    suspend fun undoPay(transactionId: String, dueDate: LocalDate) {
        val t = db.transactions().get(transactionId) ?: return
        undoPay(Payment(t, dueDate))
    }
}

data class Payment(val transaction: Transaction, val dueDate: LocalDate)
