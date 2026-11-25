package com.penny.wallet.ui

import androidx.core.content.FileProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.penny.wallet.PennyApp
import com.penny.wallet.data.prefs.SettingsStore
import com.penny.wallet.data.prefs.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * WorkManager's startup initializer does not run under Robolectric, and preferencesDataStore is one instance
 * per process, so each UI test starts from fresh settings like a new install. FileProvider also caches its roots per
 * process, which would point at the previous test's deleted data folder.
 */
class TestPennyApp : PennyApp() {
    override fun onCreate() {
        FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }.let { (it.get(null) as MutableMap<*, *>).clear() }
        if (!WorkManager.isInitialized()) WorkManager.initialize(this, Configuration.Builder().build())
        runBlocking { SettingsStore(this@TestPennyApp, CoroutineScope(Dispatchers.Unconfined)).update { UserPrefs() } }
        super.onCreate()
    }
}
