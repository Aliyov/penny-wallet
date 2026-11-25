package com.penny.wallet.data.backup

import com.penny.wallet.data.db.FIXED_TODAY
import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.DebtPayment
import com.penny.wallet.data.db.testDb
import com.penny.wallet.data.db.withSampleData
import com.penny.wallet.data.prefs.UserPrefs
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Writes the frozen fixtures for a new backup schema: -Ppenny.writeFixtures=true. Existing fixtures are never overwritten. */
@RunWith(RobolectricTestRunner::class)
class FixtureWriter {
    @Test fun write() = runBlocking {
        assumeTrue(System.getProperty("penny.writeFixtures") == "true")
        val dir = File(System.getProperty("user.dir"), "src/test/resources/backups").apply { mkdirs() }
        val plain = File(dir, "v${Backup.CURRENT_SCHEMA}-sample.json")
        val encrypted = File(dir, "v${Backup.CURRENT_SCHEMA}-sample-encrypted.json")
        check(!plain.exists() && !encrypted.exists()) { "v${Backup.CURRENT_SCHEMA} fixtures are frozen; bump Backup.CURRENT_SCHEMA first" }
        val db = testDb().withSampleData()
        val cats = db.categories().getAll().mapIndexed { i, c -> if (i == 0) c.copy(monthlyBudgetCents = 300_00) else c }
        val txs = db.transactions().getAll().mapIndexed { i, t ->
            when (i) {
                0, 1 -> t.copy(splitId = "fixture-split")
                2 -> t.copy(importId = "fixture-import")
                else -> t
            }
        }
        val day = FIXED_TODAY.minusDays(10)
        val debts = listOf(
            Debt("fixture-debt-1", "Anna", DebtDirection.I_OWE, 120_00, day, dueDate = day.plusMonths(1), remind = true, note = "Concert tickets", createdAt = 1),
            Debt("fixture-debt-2", "Marco", DebtDirection.OWED_TO_ME, 45_50, day, settledOn = FIXED_TODAY, createdAt = 2),
        )
        val payments = listOf(
            DebtPayment("fixture-pay-1", "fixture-debt-1", 20_00, day.plusDays(3), createdAt = 3),
            DebtPayment("fixture-pay-2", "fixture-debt-2", 45_50, FIXED_TODAY, transactionId = txs.first().id, createdAt = 4),
        )
        val b = Backup(
            schemaVersion = Backup.CURRENT_SCHEMA, exportedAt = "2026-11-24T12:00:00Z",
            accounts = db.accounts().getAll(), categories = cats, goals = db.goals().getAll(),
            transactions = txs, recurring = db.recurring().getAll(),
            settings = UserPrefs(currencyCode = "EUR", monthStartDay = 27, accentArgb = 0xFF00E5FF),
            debts = debts, debtPayments = payments,
        )
        plain.writeText(BackupCodec.encode(b))
        encrypted.writeText(BackupCrypto.encrypt(BackupCodec.encode(b), "fixture-pass".toCharArray(), 10_000))
        db.close()
    }
}
