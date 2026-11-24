package com.penny.wallet.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every backup file a released version could have written must keep restoring. Fixtures live in test/resources/backups. */
class BackupFixturesTest {
    private fun fixture(name: String) = javaClass.getResource("/backups/$name")!!.readText()

    private fun assertV1Sample(b: Backup) {
        assertEquals(1, b.schemaVersion)
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

    @Test fun plainV1Restores() = assertV1Sample(BackupCodec.decode(fixture("v1-sample.json"), null))

    @Test fun encryptedV1Restores() = assertV1Sample(BackupCodec.decode(fixture("v1-sample-encrypted.json"), "fixture-pass".toCharArray()))

    @Test fun encodeDecodeIsLossless() {
        val b = BackupCodec.decode(fixture("v1-sample.json"), null)
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
