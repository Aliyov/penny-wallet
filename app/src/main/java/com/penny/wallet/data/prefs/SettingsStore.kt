package com.penny.wallet.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("settings")

/**
 * Whole [UserPrefs] stored as one JSON string: adding a field needs no migration
 * (unknown/missing keys fall back to defaults) and backup/restore is trivial.
 */
class SettingsStore(private val context: Context, scope: CoroutineScope) {
    private val key = stringPreferencesKey("user_prefs_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    private val flow = context.dataStore.data.map { p -> p[key]?.let(::decode) ?: UserPrefs() }

    val prefs: StateFlow<UserPrefs> = flow.stateIn(scope, SharingStarted.Eagerly, UserPrefs())

    suspend fun current(): UserPrefs = flow.first()

    suspend fun update(transform: (UserPrefs) -> UserPrefs) {
        context.dataStore.edit { p ->
            val cur = p[key]?.let(::decode) ?: UserPrefs()
            p[key] = json.encodeToString(UserPrefs.serializer(), transform(cur))
        }
    }

    fun encode(p: UserPrefs): String = json.encodeToString(UserPrefs.serializer(), p)
    fun decode(s: String): UserPrefs = runCatching { json.decodeFromString(UserPrefs.serializer(), s) }.getOrDefault(UserPrefs())
}
