package com.penny.wallet.data.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.penny.wallet.data.db.FIXED_TODAY
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.db.testDb
import com.penny.wallet.data.prefs.SettingsStore
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.domain.csv.CsvRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImportRepoTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = testDb()
    private val settings = SettingsStore(ApplicationProvider.getApplicationContext<Context>(), scope)
    private val repo = ImportRepo(db, settings) {}

    private val rows = listOf(
        CsvRow(2, FIXED_TODAY, 12_50, false, "groceries", "Milk"),
        CsvRow(3, FIXED_TODAY, 7_00, false, "Hobbies", null),
        CsvRow(4, FIXED_TODAY, 9_99, false, null, "Unknown shop"),
        CsvRow(5, FIXED_TODAY, 2000_00, true, "Salary", "Pay"),
    )

    @Before fun setUp() = runBlocking {
        settings.update { UserPrefs() }
        db.accounts().upsert(Account("bank", "Bank", "bank", sortOrder = 0, createdAt = 0))
        db.categories().upsertAll(listOf(Category("groc", "Groceries", "groceries", 0, sortOrder = 0), Category("other", "Other", "other", 0, sortOrder = 1)))
    }

    @After fun tearDown() { db.close(); scope.cancel() }

    @Test fun importsWithCategoriesMatchedAndCreated() = runBlocking {
        val last = repo.import(rows, "bank", fallbackCategoryId = "other", createMissing = true)
        val txs = db.transactions().getAll().sortedBy { it.createdAt }
        assertEquals(4, txs.size)
        assertTrue(txs.all { it.importId == last.id && it.accountId == "bank" })
        assertEquals("groc", txs[0].categoryId)
        val hobbies = db.categories().getAll().single { it.name == "Hobbies" }
        assertEquals(hobbies.id, txs[1].categoryId)
        assertEquals("other", txs[2].categoryId)
        assertEquals(TxType.INCOME, txs[3].type)
        assertNull(txs[3].categoryId)
        assertEquals(listOf(hobbies.id), last.createdCategoryIds)
        assertEquals(last, settings.current().lastImport)
    }

    @Test fun withoutCreatingUnknownNamesGoToTheFallback() = runBlocking {
        repo.import(rows, "bank", fallbackCategoryId = "other", createMissing = false)
        assertEquals(2, db.categories().getAll().size)
        assertEquals(listOf("groc", "other", "other"), db.transactions().getAll().sortedBy { it.createdAt }.take(3).map { it.categoryId })
    }

    @Test fun undoRemovesOnlyThisImportAndItsUnusedCategories() = runBlocking {
        db.transactions().insert(com.penny.wallet.domain.t(TxType.EXPENSE, 5_00, "bank", cat = "groc"))
        val first = repo.import(rows.take(2), "bank", "other", createMissing = true)
        val second = repo.import(rows.drop(2), "bank", "other", createMissing = true)
        assertEquals(2, repo.undo(first))
        assertEquals(3, db.transactions().getAll().size)
        assertTrue(db.categories().getAll().none { it.name == "Hobbies" })
        assertEquals(second, settings.current().lastImport)
        repo.undo(second)
        assertNull(settings.current().lastImport)
        assertEquals(1, db.transactions().getAll().size)
    }

    @Test fun undoKeepsACreatedCategoryTheUserStartedUsing() = runBlocking {
        val last = repo.import(rows.take(2), "bank", "other", createMissing = true)
        val hobbies = db.categories().getAll().single { it.name == "Hobbies" }
        db.transactions().insert(com.penny.wallet.domain.t(TxType.EXPENSE, 3_00, "bank", cat = hobbies.id))
        repo.undo(last)
        assertTrue(db.categories().getAll().any { it.id == hobbies.id })
    }

    @Test fun activeCategoryWinsOverAnArchivedOneWithTheSameName() = runBlocking {
        db.categories().upsert(Category("old-groc", "Groceries", "groceries", 0, archived = true, sortOrder = 5))
        repo.import(rows.take(1), "bank", "other", createMissing = true)
        assertEquals("groc", db.transactions().getAll().single().categoryId)
    }
}
