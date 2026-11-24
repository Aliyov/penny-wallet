package com.penny.wallet.data.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the backup password on this phone so scheduled backups can be encrypted without asking.
 * It is wrapped with an Android Keystore key and kept out of [com.penny.wallet.data.prefs.UserPrefs],
 * because prefs travel inside every backup file.
 */
class BackupPasswordStore(context: Context) {
    private val prefs = context.getSharedPreferences("backup_secret", Context.MODE_PRIVATE)

    val isSet: Boolean get() = prefs.contains(KEY)

    fun get(): CharArray? = runCatching {
        val (iv, data) = prefs.getString(KEY, null)?.split(':')?.map(Base64.getDecoder()::decode) ?: return null
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(data), Charsets.UTF_8).toCharArray()
    }.getOrNull()

    fun set(password: CharArray) {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val data = cipher.doFinal(String(password).toByteArray(Charsets.UTF_8))
        val enc = Base64.getEncoder()
        prefs.edit().putString(KEY, enc.encodeToString(cipher.iv) + ":" + enc.encodeToString(data)).commit()
    }

    fun clear() {
        prefs.edit().remove(KEY).commit()
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val KEY = "password"
        const val ALIAS = "penny_backup_password"
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}
