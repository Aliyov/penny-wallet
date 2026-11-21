package com.penny.wallet.feature.settings

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

class PersonaliseViewModel(private val c: AppContainer) : ViewModel() {
    val prefs: StateFlow<UserPrefs> = c.settings.prefs
    val accounts: StateFlow<List<Account>> = c.accounts.observe().flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val defaults = UserPrefs()

    fun set(transform: (UserPrefs) -> UserPrefs) = viewModelScope.launch { c.settings.update(transform) }

    /** Keeps fields that are state rather than options (backup folder, last used, permission asked, seeded). */
    fun resetAll() = set { cur ->
        UserPrefs().copy(
            backupFolderUri = cur.backupFolderUri, lastBackupAt = cur.lastBackupAt, lastAccountId = cur.lastAccountId,
            lastCategoryId = cur.lastCategoryId, notificationPermissionAsked = cur.notificationPermissionAsked, seeded = cur.seeded,
        )
    }
}
