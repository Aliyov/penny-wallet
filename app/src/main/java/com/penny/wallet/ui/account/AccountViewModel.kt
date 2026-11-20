package com.penny.wallet.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.repo.AccountDeleteResult
import com.penny.wallet.domain.AccountMoney
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.ui.common.TxRow
import com.penny.wallet.ui.common.TxRows
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AccountAction { INCOME, WITHDRAW, SET_BALANCE, TRANSFER }

data class AccountState(
    val account: Account? = null,
    val money: AccountMoney = AccountMoney(0, 0),
    /** Other non-archived accounts, for Transfer. */
    val others: List<Account> = emptyList(),
    val rows: List<TxRow> = emptyList(),
    val missing: Boolean = false,
)

class AccountViewModel(private val c: AppContainer, private val id: String) : ViewModel() {
    val state: StateFlow<AccountState> = combine(
        c.accounts.observe(), c.categories.observe(), c.goals.observe(), c.transactions.observe(),
    ) { accounts, cats, goals, txs ->
        val a = accounts.firstOrNull { it.id == id } ?: return@combine AccountState(missing = true)
        val mine = txs.filter { it.accountId == id || it.toAccountId == id }
        AccountState(
            account = a,
            money = AccountMoney(MoneyCalculator.accountBalance(id, mine), MoneyCalculator.allocatedFrom(id, mine)),
            others = accounts.filter { !it.archived && it.id != id },
            rows = TxRows.build(mine, accounts, cats, goals, perspective = id),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountState())

    /**
     * Runs one of the account actions. [amount] is cents (for SET_BALANCE: the new balance, may be 0 or negative).
     * Calls [done] with an error message or null.
     */
    fun perform(action: AccountAction, amount: Long, date: LocalDate, note: String?, toAccountId: String?, done: (String?) -> Unit) =
        viewModelScope.launch {
            val err = runCatching {
                when (action) {
                    AccountAction.INCOME -> c.transactions.addIncome(amount, id, date, note)
                    AccountAction.WITHDRAW -> c.transactions.withdraw(amount, id, date, note)
                    AccountAction.SET_BALANCE -> c.transactions.setBalance(id, amount, date)
                    AccountAction.TRANSFER -> c.transactions.transfer(amount, id, requireNotNull(toAccountId) { "Pick an account" }, date, note)
                }
            }.exceptionOrNull()?.message
            done(err)
        }

    fun rename(name: String, icon: String) = viewModelScope.launch {
        val a = state.value.account ?: return@launch
        if (name.isNotBlank()) c.accounts.save(a.copy(name = name.trim(), icon = icon))
    }

    /** Archive is blocked while money from this account is set aside in goals (it would skew available-to-spend). */
    fun setArchived(archived: Boolean, done: (String?) -> Unit) = viewModelScope.launch {
        if (archived && state.value.money.allocated != 0L) { done("Take this account's money back from your goals first"); return@launch }
        c.accounts.setArchived(id, archived); done(null)
    }

    fun delete(done: (AccountDeleteResult) -> Unit) = viewModelScope.launch { done(c.accounts.delete(id)) }
    fun restoreAccount(a: Account) = viewModelScope.launch { c.accounts.restore(a) }

    fun deleteTx(t: Transaction, onDeleted: (Transaction) -> Unit) = viewModelScope.launch { c.transactions.delete(t); onDeleted(t) }
    fun restoreTx(t: Transaction) = viewModelScope.launch { c.transactions.restore(t) }
}
