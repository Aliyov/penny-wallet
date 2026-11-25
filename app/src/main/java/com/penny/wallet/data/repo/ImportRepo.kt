package com.penny.wallet.data.repo

import androidx.room.withTransaction
import com.penny.wallet.core.ui.theme.ItemPalette
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.prefs.LastImport
import com.penny.wallet.data.prefs.SettingsStore
import com.penny.wallet.domain.csv.CsvRow

/**
 * Writes a previewed CSV import. Every row carries the same importId, so [undo] removes exactly this import,
 * together with the categories it had to create and nothing else uses.
 */
class ImportRepo(private val db: PennyDatabase, private val settings: SettingsStore, private val listener: ChangeListener) {

    /**
     * [fallbackCategoryId] takes expenses without a category, and unknown category names unless [createMissing].
     * Category names match existing ones ignoring case.
     */
    suspend fun import(rows: List<CsvRow>, accountId: String, fallbackCategoryId: String, createMissing: Boolean): LastImport {
        val importId = newId()
        val created = mutableListOf<String>()
        db.withTransaction {
            val cats = db.categories().getAll()
            // Active categories win over archived ones with the same name.
            val byName = mutableMapOf<String, Category>()
            cats.sortedBy { it.archived }.forEach { byName.putIfAbsent(it.name.trim().lowercase(), it) }
            var order = (cats.maxOfOrNull { it.sortOrder } ?: -1) + 1
            suspend fun categoryFor(name: String?): String {
                val key = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return fallbackCategoryId
                byName[key]?.let { return it.id }
                if (!createMissing) return fallbackCategoryId
                val c = Category(newId(), name.trim(), "other", ItemPalette[order % ItemPalette.size], sortOrder = order++)
                db.categories().upsert(c)
                byName[key] = c
                created += c.id
                return c.id
            }
            val base = now()
            rows.forEachIndexed { i, r ->
                db.transactions().insert(
                    Transaction(
                        id = newId(), type = if (r.income) TxType.INCOME else TxType.EXPENSE, amountCents = r.amountCents,
                        accountId = accountId, categoryId = if (r.income) null else categoryFor(r.category),
                        date = r.date, note = r.note?.trim()?.takeIf { it.isNotEmpty() }, createdAt = base + i, importId = importId,
                    )
                )
            }
        }
        val last = LastImport(importId, rows.size, created, now())
        settings.update { it.copy(lastImport = last) }
        listener.onDataChanged()
        return last
    }

    /** Removes the import's rows and the categories it created that nothing else uses. Returns the rows removed. */
    suspend fun undo(last: LastImport): Int {
        val removed = db.withTransaction {
            val n = db.transactions().deleteImport(last.id)
            for (id in last.createdCategoryIds) {
                val used = db.transactions().countForCategory(id) > 0 || db.recurring().getAll().any { it.categoryId == id }
                if (!used) db.categories().get(id)?.let { db.categories().delete(it) }
            }
            n
        }
        settings.update { if (it.lastImport?.id == last.id) it.copy(lastImport = null) else it }
        listener.onDataChanged()
        return removed
    }
}
