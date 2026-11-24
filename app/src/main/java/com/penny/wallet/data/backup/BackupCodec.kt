package com.penny.wallet.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed class BackupProblem : Exception() {
    class NotABackup : BackupProblem()
    class TooNew(val version: Int) : BackupProblem()
    class PasswordNeeded : BackupProblem()
    class WrongPassword : BackupProblem()
}

/** Backup file text ⇄ [Backup], independent of Android so every older format can be tested on the JVM. */
object BackupCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true; coerceInputValues = true }

    fun encode(b: Backup): String = json.encodeToString(Backup.serializer(), b)

    /** The text is parsed once; the version is read before the full decode, so a newer file says "too new" instead of "not a backup". */
    fun decode(raw: String, password: CharArray?): Backup {
        var obj = parse(raw)
        if (obj["format"]?.jsonPrimitive?.contentOrNull == EncryptedBackup.FORMAT) {
            if (password == null) throw BackupProblem.PasswordNeeded()
            val plain = try {
                BackupCrypto.decrypt(raw, password)
            } catch (_: WrongPasswordException) {
                throw BackupProblem.WrongPassword()
            } catch (_: Exception) {
                throw BackupProblem.NotABackup()
            }
            obj = parse(plain)
        }
        val version = obj["schemaVersion"]?.jsonPrimitive?.intOrNull ?: throw BackupProblem.NotABackup()
        if (version > Backup.CURRENT_SCHEMA) throw BackupProblem.TooNew(version)
        if (version < 1) throw BackupProblem.NotABackup()
        return runCatching { json.decodeFromJsonElement(Backup.serializer(), obj) }.getOrElse { throw BackupProblem.NotABackup() }
    }

    private fun parse(text: String): JsonObject =
        runCatching { json.parseToJsonElement(text) as JsonObject }.getOrElse { throw BackupProblem.NotABackup() }
}
