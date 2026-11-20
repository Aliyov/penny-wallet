package com.penny.wallet.data.repo

import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.domain.MoneyCalculator
import kotlinx.coroutines.flow.Flow

sealed interface AccountDeleteResult {
    data class Deleted(val account: Account) : AccountDeleteResult
    /** Has history or money → must archive instead (never orphan transactions). */
    data object MustArchive : AccountDeleteResult
}

class AccountRepo(private val db: PennyDatabase, private val listener: ChangeListener) {
    private val dao = db.accounts()

    fun observe(): Flow<List<Account>> = dao.observeAll()
    suspend fun all() = dao.getAll()
    suspend fun get(id: String) = dao.get(id)

    suspend fun add(name: String, icon: String): Account {
        val order = (dao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        return Account(newId(), name.trim(), icon, sortOrder = order, createdAt = now()).also { dao.upsert(it); listener.onDataChanged() }
    }

    suspend fun save(a: Account) { dao.upsert(a); listener.onDataChanged() }

    suspend fun setArchived(id: String, archived: Boolean) {
        val a = dao.get(id) ?: return
        dao.upsert(a.copy(archived = archived)); listener.onDataChanged()
    }

    suspend fun reorder(idsInOrder: List<String>) {
        val byId = dao.getAll().associateBy { it.id }
        dao.upsertAll(idsInOrder.mapIndexedNotNull { i, id -> byId[id]?.copy(sortOrder = i) })
        listener.onDataChanged()
    }

    /** Deletes only an account with zero balance and no transactions referencing it. */
    suspend fun delete(id: String): AccountDeleteResult {
        val a = dao.get(id) ?: return AccountDeleteResult.MustArchive
        val txs = db.transactions().getForAccount(id)
        if (txs.isNotEmpty() || MoneyCalculator.accountBalance(id, txs) != 0L) return AccountDeleteResult.MustArchive
        dao.delete(a); listener.onDataChanged()
        return AccountDeleteResult.Deleted(a)
    }

    suspend fun restore(a: Account) { dao.upsert(a); listener.onDataChanged() }
}
