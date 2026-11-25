package com.penny.wallet.data.repo

import androidx.room.withTransaction
import com.penny.wallet.R
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.DebtPayment
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.domain.Debts
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Everything a deleted debt took with it, for Undo. */
data class DeletedDebt(val debt: Debt, val payments: List<DebtPayment>, val transactions: List<Transaction>)

/** A removed repayment, its account transaction and the debt as it was, for Undo. */
data class DeletedPayment(val payment: DebtPayment, val transaction: Transaction?, val debt: Debt)

class DebtRepo(
    private val db: PennyDatabase,
    private val listener: ChangeListener,
    private val text: (Int, String) -> String,
) {
    private val dao = db.debts()

    fun observe(): Flow<List<Debt>> = dao.observeAll()
    fun observePayments(): Flow<List<DebtPayment>> = dao.observePayments()
    suspend fun all() = dao.getAll()
    suspend fun payments() = dao.getAllPayments()
    suspend fun get(id: String) = dao.get(id)

    suspend fun add(
        person: String, direction: DebtDirection, amount: Long, date: LocalDate,
        dueDate: LocalDate? = null, remind: Boolean = false, note: String? = null,
    ): Debt {
        val d = Debt(
            newId(), requireName(person), direction, requirePositive(amount), date,
            dueDate = dueDate, remind = remind && dueDate != null, note = note?.trim()?.takeIf { it.isNotEmpty() }, createdAt = now(),
        )
        dao.upsert(d)
        listener.onDataChanged()
        return d
    }

    /**
     * Edits keep the payments. The amount may not drop below what was already paid back, and the direction can't flip
     * once there are repayments (their account transactions point the other way). A debt settled by hand stays settled.
     */
    suspend fun update(d: Debt) {
        requireName(d.person)
        requirePositive(d.amountCents)
        db.withTransaction {
            val stored = dao.get(d.id)
            val payments = dao.paymentsOf(d.id)
            val paid = payments.sumOf { it.amountCents }
            if (d.amountCents < paid) throw InvalidInputException(InvalidInputException.Reason.DEBT_BELOW_PAID)
            if (stored != null && payments.isNotEmpty() && stored.direction != d.direction) {
                throw InvalidInputException(InvalidInputException.Reason.DEBT_HAS_PAYMENTS)
            }
            val wasPaidInFull = stored != null && stored.amountCents == paid && paid > 0
            val settledOn = when {
                d.amountCents == paid -> stored?.settledOn ?: LocalDate.now()
                wasPaidInFull -> null // raising the amount of a fully paid debt opens it again
                else -> stored?.settledOn
            }
            dao.upsert(d.copy(person = d.person.trim(), remind = d.remind && d.dueDate != null, settledOn = settledOn))
        }
        listener.onDataChanged()
    }

    /**
     * A repayment. With [recordInAccount] the money also moves in that account: received → income, paid back →
     * withdrawal (not an expense: paying back isn't new spending). A debt that is fully paid becomes settled.
     */
    suspend fun addPayment(debtId: String, amount: Long, date: LocalDate, note: String? = null, recordInAccount: String? = null): DebtPayment {
        requirePositive(amount)
        val p = db.withTransaction {
            val debt = dao.get(debtId) ?: throw IllegalStateException("debt $debtId")
            val status = Debts.status(debt, dao.paymentsOf(debtId), date)
            if (amount > status.remaining) throw InvalidInputException(InvalidInputException.Reason.DEBT_OVERPAID)
            val tx = recordInAccount?.let { acc ->
                val label = text(if (debt.direction == DebtDirection.OWED_TO_ME) R.string.debt_tx_received else R.string.debt_tx_paid_back, debt.person)
                val type = if (debt.direction == DebtDirection.OWED_TO_ME) TxType.INCOME else TxType.WITHDRAWAL
                Transaction(newId(), type, amount, acc, date = date, note = label, createdAt = now()).also { db.transactions().insert(it) }
            }
            val payment = DebtPayment(newId(), debtId, amount, date, note?.trim()?.takeIf { it.isNotEmpty() }, tx?.id, now())
            dao.upsertPayment(payment)
            if (amount == status.remaining) dao.upsert(debt.copy(settledOn = date))
            payment
        }
        listener.onDataChanged()
        return p
    }

    /**
     * Removes a repayment and the account transaction recorded with it. A debt that was settled because it was paid in
     * full opens again; one settled by hand stays settled.
     */
    suspend fun deletePayment(p: DebtPayment): DeletedPayment? {
        val gone = db.withTransaction {
            val current = dao.getPayment(p.id) ?: return@withTransaction null
            val debt = dao.get(current.debtId) ?: return@withTransaction null
            val paidInFull = dao.paymentsOf(debt.id).sumOf { it.amountCents } == debt.amountCents
            val tx = current.transactionId?.let { db.transactions().get(it) }
            dao.deletePayment(current.id)
            tx?.let { db.transactions().deleteIds(listOf(it.id)) }
            if (paidInFull && debt.settledOn != null) dao.upsert(debt.copy(settledOn = null))
            DeletedPayment(current, tx, debt)
        }
        listener.onDataChanged()
        return gone
    }

    suspend fun restorePayment(g: DeletedPayment) {
        db.withTransaction {
            g.transaction?.let { db.transactions().upsertAll(listOf(it)) }
            dao.upsert(g.debt)
            dao.upsertPayment(g.payment)
        }
        listener.onDataChanged()
    }

    /** Settled by hand, e.g. forgiven, or paid back outside the app. */
    suspend fun setSettled(id: String, settled: Boolean, on: LocalDate = LocalDate.now()) {
        dao.get(id)?.let { dao.upsert(it.copy(settledOn = if (settled) on else null)) }
        listener.onDataChanged()
    }

    /** Deletes the debt, its repayments and the account transactions recorded with them. */
    suspend fun delete(id: String): DeletedDebt? {
        val gone = db.withTransaction {
            val d = dao.get(id) ?: return@withTransaction null
            val payments = dao.paymentsOf(id)
            val txIds = payments.mapNotNull { it.transactionId }
            val txs = if (txIds.isEmpty()) emptyList() else db.transactions().getIds(txIds)
            if (txIds.isNotEmpty()) db.transactions().deleteIds(txIds)
            dao.delete(id) // payments go with it (foreign key cascade)
            DeletedDebt(d, payments, txs)
        }
        listener.onDataChanged()
        return gone
    }

    suspend fun restore(g: DeletedDebt) {
        db.withTransaction {
            dao.upsert(g.debt)
            db.transactions().upsertAll(g.transactions)
            dao.upsertPayments(g.payments)
        }
        listener.onDataChanged()
    }

    private fun requireName(s: String): String = s.trim().ifEmpty { throw InvalidInputException(InvalidInputException.Reason.NAME_REQUIRED) }
    private fun requirePositive(a: Long): Long { if (a <= 0) throw InvalidInputException(InvalidInputException.Reason.AMOUNT_NOT_POSITIVE); return a }
}
