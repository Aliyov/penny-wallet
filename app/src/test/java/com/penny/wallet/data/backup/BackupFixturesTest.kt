package com.penny.wallet.data.backup

import com.penny.wallet.data.db.FIXED_TODAY
import com.penny.wallet.data.db.entities.DebtDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every backup file a released version could have written must keep restoring. Fixtures live in test/resources/backups. */
class BackupFixturesTest {
    private fun fixture(name: String) = javaClass.getResource("/backups/$name")!!.readText()

    /** The sample data every fixture holds; older versions arrive upgraded to the current schema. */
    private fun assertV1Sample(b: Backup) {
        assertEquals(Backup.CURRENT_SCHEMA, b.schemaVersion)
        assertEquals(2, b.accounts.size)
        assertTrue(b.categories.isNotEmpty())
        assertEquals(2, b.goals.size)
        assertEquals(2, b.recurring.size)
        assertTrue(b.transactions.size >= 20)
        assertEquals(27, b.settings?.monthStartDay)
        assertEquals("EUR", b.settings?.currencyCode)
        val ids = b.accounts.map { it.id }.toSet()
        assertTrue(b.transactions.all { it.accountId in ids })
    }

    @Test fun plainV1Restores() {
        val b = BackupCodec.decode(fixture("v1-sample.json"), null)
        assertV1Sample(b)
        assertTrue(b.debts.isEmpty() && b.debtPayments.isEmpty())
        assertTrue(b.categories.all { it.monthlyBudgetCents == null })
        assertTrue(b.transactions.all { it.splitId == null && it.importId == null })
    }

    private fun assertV2Sample(b: Backup) {
        assertV1Sample(b)
        assertEquals(30000L, b.categories.first().monthlyBudgetCents)
        assertEquals(2, b.transactions.count { it.splitId == "fixture-split" })
        assertEquals(1, b.transactions.count { it.importId == "fixture-import" })
        assertEquals(listOf("Anna", "Marco"), b.debts.map { it.person })
        assertEquals(DebtDirection.I_OWE, b.debts[0].direction)
        assertTrue(b.debts[0].remind)
        assertEquals(FIXED_TODAY, b.debts[1].settledOn)
        assertEquals(listOf(2000L, 4550L), b.debtPayments.map { it.amountCents })
        assertEquals(b.transactions.first().id, b.debtPayments[1].transactionId)
    }

    @Test fun plainV2Restores() = assertV2Sample(BackupCodec.decode(fixture("v2-sample.json"), null))

    @Test fun encryptedV2Restores() = assertV2Sample(BackupCodec.decode(fixture("v2-sample-encrypted.json"), "fixture-pass".toCharArray()))

    @Test fun encryptedV1Restores() = assertV1Sample(BackupCodec.decode(fixture("v1-sample-encrypted.json"), "fixture-pass".toCharArray()))

    @Test fun encodeDecodeIsLossless() {
        val b = BackupCodec.decode(fixture("v2-sample.json"), null)
        assertEquals(b, BackupCodec.decode(BackupCodec.encode(b), null))
    }

    @Test(expected = BackupProblem.PasswordNeeded::class)
    fun encryptedWithoutPassword() { BackupCodec.decode(fixture("v1-sample-encrypted.json"), null) }

    @Test(expected = BackupProblem.WrongPassword::class)
    fun encryptedWrongPassword() { BackupCodec.decode(fixture("v1-sample-encrypted.json"), "nope".toCharArray()) }

    @Test fun newerSchemaIsRefusedAsTooNew() {
        val newer = fixture("v1-sample.json").replaceFirst("\"schemaVersion\": 1", "\"schemaVersion\": ${Backup.CURRENT_SCHEMA + 1}")
        try {
            BackupCodec.decode(newer, null); error("accepted")
        } catch (e: BackupProblem.TooNew) {
            assertEquals(Backup.CURRENT_SCHEMA + 1, e.version)
        }
    }

    @Test fun unknownFieldsFromNewerMinorVersionsAreIgnored() {
        val extra = fixture("v1-sample.json").replaceFirst("{", "{\"futureField\": [1, 2],")
        assertV1Sample(BackupCodec.decode(extra, null))
    }

    @Test fun garbageIsNotABackup() {
        val inputs = listOf("", " ", "null", "[]", "{}", "{\"schemaVersion\": \"x\"}", "{\"schemaVersion\": 0}",
            "{\"schemaVersion\": 1}", "PK\u0003\u0004", fixture("v1-sample.json").take(5000))
        for (raw in inputs) {
            try {
                BackupCodec.decode(raw, null); error("accepted: ${raw.take(40)}")
            } catch (_: BackupProblem.NotABackup) {
            }
        }
    }

    @Test fun fuzzNeverCrashes() {
        val base = fixture("v1-sample.json")
        val rnd = kotlin.random.Random(7)
        repeat(2_000) {
            val chars = base.toCharArray()
            repeat(1 + rnd.nextInt(20)) { chars[rnd.nextInt(chars.size)] = (rnd.nextInt(32, 127)).toChar() }
            try {
                BackupCodec.decode(String(chars), null)
            } catch (_: BackupProblem) {
            }
        }
    }
}
