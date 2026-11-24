package com.penny.wallet.data.backup

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
        val b = Backup(
            schemaVersion = Backup.CURRENT_SCHEMA, exportedAt = "2026-11-24T12:00:00Z",
            accounts = db.accounts().getAll(), categories = db.categories().getAll(), goals = db.goals().getAll(),
            transactions = db.transactions().getAll(), recurring = db.recurring().getAll(),
            settings = UserPrefs(currencyCode = "EUR", monthStartDay = 27, accentArgb = 0xFF00E5FF),
        )
        plain.writeText(BackupCodec.encode(b))
        encrypted.writeText(BackupCrypto.encrypt(BackupCodec.encode(b), "fixture-pass".toCharArray(), 10_000))
        db.close()
    }
}
