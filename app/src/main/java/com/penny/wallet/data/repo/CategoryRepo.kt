package com.penny.wallet.data.repo

import androidx.room.withTransaction
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.db.entities.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepo(private val db: PennyDatabase, private val listener: ChangeListener) {
    private val dao = db.categories()

    fun observe(): Flow<List<Category>> = dao.observeAll()
    suspend fun all() = dao.getAll()

    suspend fun add(name: String, icon: String, color: Long): Category {
        val order = (dao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        return Category(newId(), name.trim(), icon, color, sortOrder = order).also { dao.upsert(it); listener.onDataChanged() }
    }

    suspend fun save(c: Category) { dao.upsert(c); listener.onDataChanged() }

    suspend fun setArchived(id: String, archived: Boolean) {
        val c = dao.get(id) ?: return
        dao.upsert(c.copy(archived = archived)); listener.onDataChanged()
    }

    suspend fun reorder(idsInOrder: List<String>) {
        val byId = dao.getAll().associateBy { it.id }
        dao.upsertAll(idsInOrder.mapIndexedNotNull { i, id -> byId[id]?.copy(sortOrder = i) })
        listener.onDataChanged()
    }

    suspend fun expenseCount(id: String) = db.transactions().countForCategory(id)

    /** Deletes a category with no expenses. Returns null if it has expenses (caller offers move/archive). */
    suspend fun deleteIfUnused(id: String): Category? {
        val c = dao.get(id) ?: return null
        if (db.transactions().countForCategory(id) > 0) return null
        if (db.recurring().getAll().any { it.categoryId == id }) return null
        dao.delete(c); listener.onDataChanged()
        return c
    }

    /** Moves all expenses + recurring rules to [targetId], then deletes. Returns ids moved for undo. */
    suspend fun moveAndDelete(id: String, targetId: String): MovedCategory? = db.withTransaction {
        val c = dao.get(id) ?: return@withTransaction null
        val movedTx = db.transactions().getAll().filter { it.categoryId == id }.map { it.id }
        val movedRules = db.recurring().getAll().filter { it.categoryId == id }
        db.transactions().moveCategory(id, targetId)
        db.recurring().upsertAll(movedRules.map { it.copy(categoryId = targetId) })
        dao.delete(c)
        MovedCategory(c, movedTx, movedRules.map { it.id })
    }.also { listener.onDataChanged() }

    suspend fun undoMove(m: MovedCategory) {
        db.withTransaction {
            dao.upsert(m.category)
            val txs = db.transactions().getAll().filter { it.id in m.txIds.toSet() }
            db.transactions().upsertAll(txs.map { it.copy(categoryId = m.category.id) })
            val rules = db.recurring().getAll().filter { it.id in m.ruleIds.toSet() }
            db.recurring().upsertAll(rules.map { it.copy(categoryId = m.category.id) })
        }
        listener.onDataChanged()
    }

    suspend fun restore(c: Category) { dao.upsert(c); listener.onDataChanged() }
}

data class MovedCategory(val category: Category, val txIds: List<String>, val ruleIds: List<String>)
