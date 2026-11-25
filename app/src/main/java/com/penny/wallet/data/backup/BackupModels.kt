package com.penny.wallet.data.backup

import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtPayment
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.prefs.UserPrefs
import kotlinx.serialization.Serializable

/**
 * Bump [CURRENT_SCHEMA] whenever a release adds data, freeze a fixture of the new version and keep reading
 * every older one ([BackupMigrations]). Older app versions refuse a newer file as "too new".
 */
@Serializable
data class Backup(
    val schemaVersion: Int,
    /** ISO-8601 instant. */
    val exportedAt: String,
    val app: String = "Penny Wallet",
    val accounts: List<Account>,
    val categories: List<Category>,
    val goals: List<Goal>,
    val transactions: List<Transaction>,
    val recurring: List<RecurringExpense>,
    val settings: UserPrefs? = null,
    /** Since schema 2. */
    val debts: List<Debt> = emptyList(),
    /** Since schema 2. */
    val debtPayments: List<DebtPayment> = emptyList(),
) {
    companion object { const val CURRENT_SCHEMA = 2 }
}

data class BackupSummary(
    val exportedAt: String,
    val schemaVersion: Int,
    val accounts: Int,
    val categories: Int,
    val goals: Int,
    val transactions: Int,
    val recurring: Int,
    val debts: Int,
    val firstDate: String?,
    val lastDate: String?,
    /** Rows referencing missing accounts/categories/goals; restored anyway, shown as a warning. */
    val danglingRefs: Int,
)

sealed interface BackupResult {
    data class Ok(val fileName: String, val at: Long) : BackupResult
    data class Failed(val reason: String) : BackupResult
}
