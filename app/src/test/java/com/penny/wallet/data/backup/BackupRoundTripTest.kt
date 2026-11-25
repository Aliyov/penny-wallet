package com.penny.wallet.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.penny.wallet.data.db.FIXED_TODAY
import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.DebtPayment
import com.penny.wallet.data.db.testDb
import com.penny.wallet.data.db.withSampleData
import com.penny.wallet.data.prefs.SettingsStore
import com.penny.wallet.data.prefs.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = testDb()
    private val settings = SettingsStore(ctx, scope)
    private var changes = 0
    private val manager = BackupManager(ctx, db, settings) { changes++ }

    /** preferencesDataStore is one instance per process, so settings would otherwise leak between tests. */
    @Before fun resetSettings() = runBlocking { settings.update { UserPrefs() } }

    @After fun tearDown() { db.close(); scope.cancel() }

    @Test fun backupRestoreGivesIdenticalData() = runBlocking {
        db.withSampleData()
        db.debts().upsert(Debt("d1", "Anna", DebtDirection.I_OWE, 120_00, FIXED_TODAY, note = "Tickets", createdAt = 1))
        db.debts().upsertPayment(DebtPayment("p1", "d1", 20_00, FIXED_TODAY, createdAt = 2))
        settings.update { it.copy(currencyCode = "JPY", monthStartDay = 27) }
        val before = manager.snapshot()
        val text = BackupCodec.encode(before)

        manager.resetAll()
        assertTrue(db.transactions().getAll().isEmpty())
        assertTrue(db.debts().getAll().isEmpty() && db.debts().getAllPayments().isEmpty())

        manager.restore(BackupCodec.decode(text, null))
        val after = manager.snapshot()
        assertEquals(before.accounts, after.accounts)
        assertEquals(before.categories, after.categories)
        assertEquals(before.goals, after.goals)
        assertEquals(before.transactions.sortedBy { it.id }, after.transactions.sortedBy { it.id })
        assertEquals(before.recurring, after.recurring)
        assertEquals(before.debts, after.debts)
        assertEquals(before.debtPayments, after.debtPayments)
        assertEquals("JPY", settings.current().currencyCode)
        assertEquals(27, settings.current().monthStartDay)
        assertTrue(changes >= 2)
    }

    @Test fun everyFixtureRestoresIntoCurrentDatabase() = runBlocking {
        for (v in 1..Backup.CURRENT_SCHEMA) {
            val b = BackupCodec.decode(javaClass.getResource("/backups/v$v-sample.json")!!.readText(), null)
            manager.restore(b)
            assertEquals(b.transactions.size, db.transactions().getAll().size)
            assertEquals(b.debts.size, db.debts().getAll().size)
            assertEquals(0, manager.summarize(b).danglingRefs)
            assertEquals(27, settings.current().monthStartDay)
        }
    }

    @Test fun restoreReplacesExistingData() = runBlocking {
        db.withSampleData()
        val empty = Backup(Backup.CURRENT_SCHEMA, "2026-01-01T00:00:00Z", accounts = emptyList(), categories = emptyList(),
            goals = emptyList(), transactions = emptyList(), recurring = emptyList())
        manager.restore(empty)
        assertTrue(db.accounts().getAll().isEmpty())
        assertTrue(db.transactions().getAll().isEmpty())
    }

    @Test fun brokenDebtReferencesAreCountedAndRepairedOnRestore() = runBlocking {
        val b = BackupCodec.decode(javaClass.getResource("/backups/v2-sample.json")!!.readText(), null)
        val broken = b.copy(debtPayments = b.debtPayments + listOf(
            DebtPayment("orphan", "no-such-debt", 1_00, FIXED_TODAY, createdAt = 10),
            DebtPayment("lost-tx", b.debts.first().id, 2_00, FIXED_TODAY, transactionId = "no-such-tx", createdAt = 11),
        ))
        assertEquals(2, manager.summarize(broken).danglingRefs)
        manager.restore(broken)
        val payments = db.debts().getAllPayments().associateBy { it.id }
        assertTrue("orphan" !in payments)
        assertEquals(null, payments.getValue("lost-tx").transactionId)
        assertEquals(b.debtPayments.size + 1, payments.size)
    }

    @Test fun summaryShowsTheFileVersionAndDebts() {
        val v1 = javaClass.getResource("/backups/v1-sample.json")!!.readText()
        val (b1, ver1) = BackupCodec.decodeFile(v1, null)
        assertEquals(1, manager.summarize(b1, ver1).schemaVersion)
        assertEquals(0, manager.summarize(b1, ver1).debts)
        val b2 = BackupCodec.decode(javaClass.getResource("/backups/v2-sample.json")!!.readText(), null)
        assertEquals(2, manager.summarize(b2).debts)
    }
}
