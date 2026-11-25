package com.penny.wallet.data.repo

import com.penny.wallet.R
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.domain.SplitPart
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TransactionRepo(
    private val db: PennyDatabase,
    private val listener: ChangeListener,
    private val text: (Int) -> String,
) {
    private val dao = db.transactions()

    fun observe(): Flow<List<Transaction>> = dao.observeAll()
    fun observeForAccount(id: String): Flow<List<Transaction>> = dao.observeForAccount(id)
    suspend fun all() = dao.getAll()
    suspend fun get(id: String) = dao.get(id)

    private suspend fun insert(t: Transaction): Transaction { dao.insert(t); listener.onDataChanged(); return t }

    private fun tx(type: TxType, amount: Long, account: String, date: LocalDate, note: String?, to: String? = null,
                   category: String? = null, goal: String? = null, recurring: String? = null) =
        Transaction(newId(), type, amount, account, to, category, goal, recurring, date, note?.takeIf { it.isNotBlank() }, now())

    suspend fun addExpense(amount: Long, accountId: String, categoryId: String, date: LocalDate, note: String? = null, recurringId: String? = null) =
        insert(tx(TxType.EXPENSE, requirePositive(amount), accountId, date, note, category = categoryId, recurring = recurringId))

    suspend fun addIncome(amount: Long, accountId: String, date: LocalDate, note: String? = null) =
        insert(tx(TxType.INCOME, requirePositive(amount), accountId, date, note))

    suspend fun withdraw(amount: Long, accountId: String, date: LocalDate, note: String? = null) =
        insert(tx(TxType.WITHDRAWAL, requirePositive(amount), accountId, date, note))

    suspend fun transfer(amount: Long, from: String, to: String, date: LocalDate, note: String? = null): Transaction {
        if (from == to) throw InvalidInputException(InvalidInputException.Reason.SAME_ACCOUNTS)
        return insert(tx(TxType.TRANSFER, requirePositive(amount), from, date, note, to = to))
    }

    /** Set balance to X → ADJUSTMENT of X − current. Returns null when already equal. */
    suspend fun setBalance(accountId: String, target: Long, date: LocalDate): Transaction? {
        val current = MoneyCalculator.accountBalance(accountId, dao.getForAccount(accountId))
        val diff = MoneyCalculator.adjustmentFor(target, current)
        if (diff == 0L) return null
        return insert(tx(TxType.ADJUSTMENT, diff, accountId, date, text(R.string.data_note_balance_set)))
    }

    /**
     * Earmark money from an account into a goal. Can't exceed what's available in that account.
     * With [MoneyCalculator.EXTERNAL] the money comes from outside, so no balance is touched or checked.
     */
    suspend fun save(amount: Long, accountId: String, goalId: String, date: LocalDate): Transaction {
        requirePositive(amount)
        if (accountId == MoneyCalculator.EXTERNAL) return insert(tx(TxType.SAVE, amount, accountId, date, null, goal = goalId))
        val available = MoneyCalculator.availableInAccount(accountId, dao.getAll())
        if (amount > available) throw InsufficientAvailableException(available)
        return insert(tx(TxType.SAVE, amount, accountId, date, null, goal = goalId))
    }

    /** Take money back from a goal to an account. Can't exceed the goal's balance. */
    suspend fun unsave(amount: Long, accountId: String, goalId: String, date: LocalDate): Transaction {
        requirePositive(amount)
        val inGoal = MoneyCalculator.goalBalance(goalId, dao.getForGoal(goalId))
        if (amount > inGoal) throw InsufficientAvailableException(inGoal)
        return insert(tx(TxType.UNSAVE, amount, accountId, date, null, goal = goalId))
    }

    /**
     * One receipt over several categories: one expense per part, sharing a splitId, account, date and note, so every
     * total, budget and chart counts the parts like ordinary expenses.
     */
    suspend fun addSplit(parts: List<SplitPart>, accountId: String, date: LocalDate, note: String?): List<Transaction> {
        val splitId = newId()
        val rows = splitRows(parts, accountId, date, note, splitId)
        db.withTransaction { rows.forEach { dao.insert(it) } }
        listener.onDataChanged()
        return rows
    }

    /**
     * Edit of a split: the parts keep their ids and creation times by position (so ordering and references stay put);
     * extra new parts are added, parts no longer there are removed.
     */
    suspend fun replaceSplit(splitId: String, parts: List<SplitPart>, accountId: String, date: LocalDate, note: String?): List<Transaction> {
        val fresh = splitRows(parts, accountId, date, note, splitId)
        val rows = db.withTransaction {
            val old = dao.getSplit(splitId)
            val rows = fresh.mapIndexed { i, r ->
                old.getOrNull(i)?.let { o -> r.copy(id = o.id, createdAt = o.createdAt, recurringId = o.recurringId, importId = o.importId) } ?: r
            }
            val gone = old.drop(rows.size).map { it.id }
            if (gone.isNotEmpty()) dao.deleteIds(gone)
            dao.upsertAll(rows)
            rows
        }
        listener.onDataChanged()
        return rows
    }

    suspend fun splitParts(splitId: String): List<Transaction> = dao.getSplit(splitId)

    private fun splitRows(parts: List<SplitPart>, accountId: String, date: LocalDate, note: String?, splitId: String): List<Transaction> {
        if (parts.size < 2) throw InvalidInputException(InvalidInputException.Reason.SPLIT_TOO_FEW)
        if (parts.any { it.categoryId == null }) throw InvalidInputException(InvalidInputException.Reason.SPLIT_NO_CATEGORY)
        val base = now()
        return parts.mapIndexed { i, p ->
            tx(TxType.EXPENSE, requirePositive(p.amountCents), accountId, date, note, category = p.categoryId)
                .copy(splitId = splitId, createdAt = base + i)
        }
    }

    suspend fun update(t: Transaction) { dao.upsert(t); listener.onDataChanged() }

    // Parts of a split deleted together, kept for the undo that follows (the snackbar only ever holds one row).
    // Only the last few: older ones can no longer be undone anyway.
    private val deletedSplits = object : LinkedHashMap<String, List<Transaction>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Transaction>>?) = size > 4
    }

    /** Deleting any part of a split deletes the whole split. */
    suspend fun delete(t: Transaction) {
        val split = t.splitId
        if (split == null) {
            dao.delete(t)
        } else {
            db.withTransaction {
                val parts = dao.getSplit(split).ifEmpty { listOf(t) }
                synchronized(deletedSplits) { deletedSplits[split] = parts }
                dao.deleteSplit(split)
            }
        }
        listener.onDataChanged()
    }

    /**
     * Undo of a delete: re-inserts the exact same row, or every part of its split. When the parts are no longer known
     * (the app was restarted in between) the one row comes back as an ordinary expense, never as a one-part split.
     */
    suspend fun restore(t: Transaction) {
        val split = t.splitId
        val known = split?.let { synchronized(deletedSplits) { deletedSplits.remove(it) } }
        db.withTransaction {
            when {
                split == null -> dao.upsert(t)
                known != null -> dao.upsertAll(known)
                dao.getSplit(split).isEmpty() -> dao.upsert(t.copy(splitId = null))
                else -> dao.upsert(t)
            }
        }
        listener.onDataChanged()
    }

    private fun requirePositive(a: Long): Long { if (a <= 0) throw InvalidInputException(InvalidInputException.Reason.AMOUNT_NOT_POSITIVE); return a }
}
