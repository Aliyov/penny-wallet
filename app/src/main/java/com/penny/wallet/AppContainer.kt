package com.penny.wallet

import android.content.Context
import com.penny.wallet.data.backup.BackupManager
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.ocr.ReceiptReader
import com.penny.wallet.data.prefs.SettingsStore
import com.penny.wallet.data.repo.AccountRepo
import com.penny.wallet.data.repo.CategoryRepo
import com.penny.wallet.data.repo.DebtRepo
import com.penny.wallet.data.repo.ChangeListener
import com.penny.wallet.data.repo.GoalRepo
import com.penny.wallet.data.repo.RecurringRepo
import com.penny.wallet.data.repo.SetupRepo
import com.penny.wallet.data.repo.TransactionRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db: PennyDatabase = PennyDatabase.build(context)
    val settings = SettingsStore(context, appScope)

    @Volatile var onDataChanged: () -> Unit = {}
    private val listener = ChangeListener { onDataChanged() }

    val accounts = AccountRepo(db, listener)
    val categories = CategoryRepo(db, listener)
    val goals = GoalRepo(db, listener) { id, arg -> context.getString(id, arg) }
    val transactions = TransactionRepo(db, listener, context::getString)
    val recurring = RecurringRepo(db, listener)
    val debts = DebtRepo(db, listener) { id, arg -> context.getString(id, arg) }
    val backup = BackupManager(context, db, settings, listener)
    val receipts = ReceiptReader(context)
    val setup = SetupRepo(context, db, settings, listener)
}
