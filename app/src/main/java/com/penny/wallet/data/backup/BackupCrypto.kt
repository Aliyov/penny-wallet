package com.penny.wallet.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-protected backup file: a small JSON envelope around the AES-GCM encrypted backup JSON.
 * The `format` field tells restore to ask for the password; plain backups have no such field.
 */
@Serializable
data class EncryptedBackup(
    val app: String = "Penny Wallet",
    val format: String = FORMAT,
    val version: Int = 1,
    val kdf: String = KDF,
    val iterations: Int,
    val salt: String,
    val iv: String,
    val data: String,
) {
    companion object {
        const val FORMAT = "penny-wallet-encrypted"
        const val KDF = "PBKDF2WithHmacSHA256"
    }
}

class WrongPasswordException : Exception()

object BackupCrypto {
    const val MIN_PASSWORD = 8
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val random = SecureRandom()
    private val b64 = Base64.getEncoder()
    private val unb64 = Base64.getDecoder()

    fun isEncrypted(text: String): Boolean = runCatching {
        json.parseToJsonElement(text).jsonObject["format"]?.jsonPrimitive?.content == EncryptedBackup.FORMAT
    }.getOrDefault(false)

    fun encrypt(plain: String, password: CharArray, iterations: Int = ITERATIONS): String {
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(EncryptedBackup.FORMAT.toByteArray())
        val data = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return json.encodeToString(
            EncryptedBackup.serializer(),
            EncryptedBackup(iterations = iterations, salt = b64.encodeToString(salt), iv = b64.encodeToString(iv), data = b64.encodeToString(data)),
        )
    }

    /** Throws [WrongPasswordException] when the password doesn't match (or the file was altered). */
    fun decrypt(text: String, password: CharArray): String {
        val e = json.decodeFromString(EncryptedBackup.serializer(), text)
        require(e.format == EncryptedBackup.FORMAT && e.kdf == EncryptedBackup.KDF && e.version == 1 && e.iterations in 10_000..2_000_000)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, unb64.decode(e.salt), e.iterations), GCMParameterSpec(TAG_BITS, unb64.decode(e.iv)))
        cipher.updateAAD(EncryptedBackup.FORMAT.toByteArray())
        return try {
            cipher.doFinal(unb64.decode(e.data)).toString(Charsets.UTF_8)
        } catch (_: AEADBadTagException) {
            throw WrongPasswordException()
        }
    }

    private fun key(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance(EncryptedBackup.KDF).generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
