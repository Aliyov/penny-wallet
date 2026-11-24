package com.penny.wallet.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    private val plain = """{"schemaVersion":1,"exportedAt":"2026-11-24T10:00:00Z","accounts":[],"note":"Café 12,50 €"}"""
    private val fast = 10_000

    @Test fun roundTrip() {
        val enc = BackupCrypto.encrypt(plain, "correct horse".toCharArray(), fast)
        assertTrue(BackupCrypto.isEncrypted(enc))
        assertFalse(enc.contains("exportedAt"))
        assertEquals(plain, BackupCrypto.decrypt(enc, "correct horse".toCharArray()))
    }

    @Test(expected = WrongPasswordException::class)
    fun wrongPassword() {
        val enc = BackupCrypto.encrypt(plain, "correct horse".toCharArray(), fast)
        BackupCrypto.decrypt(enc, "wrong horse".toCharArray())
    }

    @Test(expected = WrongPasswordException::class)
    fun tamperedDataIsRejected() {
        val enc = BackupCrypto.encrypt(plain, "pw123456".toCharArray(), fast)
        val data = Regex("\"data\": \"([^\"]+)\"").find(enc)!!.groupValues[1]
        val flipped = (if (data[5] == 'A') 'B' else 'A') + ""
        BackupCrypto.decrypt(enc.replace(data, data.substring(0, 5) + flipped + data.substring(6)), "pw123456".toCharArray())
    }

    @Test fun saltMakesEveryFileDifferent() =
        assertNotEquals(BackupCrypto.encrypt(plain, "pw123456".toCharArray(), fast), BackupCrypto.encrypt(plain, "pw123456".toCharArray(), fast))

    @Test fun plainBackupIsNotEncrypted() {
        assertFalse(BackupCrypto.isEncrypted(plain))
        assertFalse(BackupCrypto.isEncrypted("not json"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun absurdIterationCountIsRefused() {
        val enc = BackupCrypto.encrypt(plain, "pw123456".toCharArray(), fast).replace("\"iterations\": 10000", "\"iterations\": 999999999")
        BackupCrypto.decrypt(enc, "pw123456".toCharArray())
    }
}
