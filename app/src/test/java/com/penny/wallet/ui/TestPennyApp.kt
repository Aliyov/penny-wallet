package com.penny.wallet.ui

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
 * per process, so each UI test starts from fresh settings like a new install.
 */
class TestPennyApp : PennyApp() {
    override fun onCreate() {
        WorkManager.initialize(this, Configuration.Builder().build())
        runBlocking { SettingsStore(this@TestPennyApp, CoroutineScope(Dispatchers.Unconfined)).update { UserPrefs() } }
        super.onCreate()
    }
}
