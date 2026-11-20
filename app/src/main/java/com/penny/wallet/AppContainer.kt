package com.penny.wallet

import android.content.Context
import com.penny.wallet.data.backup.BackupManager
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.ocr.ReceiptReader
import com.penny.wallet.data.prefs.SettingsStore
import com.penny.wallet.data.repo.AccountRepo
import com.penny.wallet.data.repo.CategoryRepo
import com.penny.wallet.data.repo.ChangeListener
import com.penny.wallet.data.repo.GoalRepo
import com.penny.wallet.data.repo.RecurringRepo
import com.penny.wallet.data.repo.TransactionRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/** Manual DI (SPEC §2). One instance, owned by [PennyApp]. */
class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: PennyDatabase = PennyDatabase.build(context)
    val settings = SettingsStore(context, appScope)

    /** Widgets register here (phase 10); repos call it after every write. */
    @Volatile var onDataChanged: () -> Unit = {}
    private val listener = ChangeListener { onDataChanged() }

    val accounts = AccountRepo(db, listener)
    val categories = CategoryRepo(db, listener)
    val goals = GoalRepo(db, listener)
    val transactions = TransactionRepo(db, listener)
    val recurring = RecurringRepo(db, listener)
    val backup = BackupManager(context, db, settings, listener)
    val receipts = ReceiptReader(context)
}
