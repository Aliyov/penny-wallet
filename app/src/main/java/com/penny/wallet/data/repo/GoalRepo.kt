package com.penny.wallet.data.repo

import androidx.room.withTransaction
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.domain.MoneyCalculator
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Snapshot to undo a goal delete: the goal and the UNSAVE rows created by the delete. */
data class DeletedGoal(val goal: Goal, val returned: List<Transaction>, val history: List<Transaction>)

class GoalRepo(private val db: PennyDatabase, private val listener: ChangeListener) {
    private val dao = db.goals()

    fun observe(): Flow<List<Goal>> = dao.observeAll()
    suspend fun all() = dao.getAll()
    suspend fun get(id: String) = dao.get(id)

    suspend fun add(name: String, icon: String, color: Long, targetCents: Long, deadline: LocalDate?): Goal {
        require(targetCents > 0) { "Target must be greater than zero" }
        val order = (dao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        return Goal(newId(), name.trim(), icon, color, targetCents, deadline, sortOrder = order, createdAt = now())
            .also { dao.upsert(it); listener.onDataChanged() }
    }

    suspend fun save(g: Goal) { require(g.targetCents > 0); dao.upsert(g); listener.onDataChanged() }

    suspend fun reorder(idsInOrder: List<String>) {
        val byId = dao.getAll().associateBy { it.id }
        dao.upsertAll(idsInOrder.mapIndexedNotNull { i, id -> byId[id]?.copy(sortOrder = i) })
        listener.onDataChanged()
    }

    /**
     * Returns the goal's money to its source accounts via UNSAVE (SPEC §4), then deletes the goal.
     * The goal's SAVE/UNSAVE history is removed with it so no transaction points at a missing goal;
     * the account balances are unaffected because SAVE/UNSAVE are earmarks only.
     */
    suspend fun delete(id: String, today: LocalDate = LocalDate.now()): DeletedGoal? = db.withTransaction {
        val g = dao.get(id) ?: return@withTransaction null
        val tdao = db.transactions()
        val history = tdao.getForGoal(id)
        val split = MoneyCalculator.unsaveSplit(id, history)
        val returned = split.map { (acc, amt) ->
            Transaction(newId(), TxType.UNSAVE, amt, acc, goalId = id, date = today, note = "Returned from ${g.name}", createdAt = now())
        }
        // Net effect on every account's allocation equals adding `returned`; we drop the whole goal ledger.
        tdao.deleteIds(history.map { it.id })
        dao.delete(g)
        DeletedGoal(g, returned, history)
    }.also { listener.onDataChanged() }

    suspend fun undoDelete(d: DeletedGoal) {
        db.withTransaction { dao.upsert(d.goal); db.transactions().upsertAll(d.history) }
        listener.onDataChanged()
    }
}
