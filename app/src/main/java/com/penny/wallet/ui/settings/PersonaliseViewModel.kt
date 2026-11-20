package com.penny.wallet.ui.settings

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.prefs.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Every §7a option goes through [set]; changes apply live via SettingsStore's StateFlow. */
class PersonaliseViewModel(private val c: AppContainer) : ViewModel() {
    val prefs: StateFlow<UserPrefs> = c.settings.prefs
    val accounts: StateFlow<List<Account>> = c.accounts.observe().flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val defaults = UserPrefs()

    fun set(transform: (UserPrefs) -> UserPrefs) = viewModelScope.launch { c.settings.update(transform) }

    /** Resets only the user-facing options; keeps data-ish fields (folder, last used, permission, seeded). */
    fun resetAll() = set { cur ->
        UserPrefs().copy(
            backupFolderUri = cur.backupFolderUri, lastBackupAt = cur.lastBackupAt, lastAccountId = cur.lastAccountId,
            lastCategoryId = cur.lastCategoryId, notificationPermissionAsked = cur.notificationPermissionAsked, seeded = cur.seeded,
        )
    }
}
