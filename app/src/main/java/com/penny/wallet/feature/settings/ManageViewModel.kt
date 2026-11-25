package com.penny.wallet.feature.settings

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.R
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.repo.AccountDeleteResult
import com.penny.wallet.data.repo.MovedCategory
import com.penny.wallet.domain.AccountMoney
import com.penny.wallet.domain.MoneyCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryItem(val category: Category, val expenseCount: Int, val spentTotal: Long)
data class AccountItem(val account: Account, val money: AccountMoney, val txCount: Int)

data class ManageState(
    val categories: List<CategoryItem> = emptyList(),
    val accounts: List<AccountItem> = emptyList(),
)

/** What deleting a category with expenses needs from the user. */
sealed interface CategoryDelete {
    data class Deleted(val category: Category) : CategoryDelete
    /** Has expenses or recurring rules: ask "Move expenses to…" or archive. */
    data class InUse(val category: Category, val count: Int) : CategoryDelete
}

class ManageViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<ManageState> = combine(
        c.categories.observe(), c.accounts.observe(), c.transactions.observe(), c.goals.observe(),
    ) { cats, accounts, txs, goals ->
        val byCat = txs.filter { it.categoryId != null }.groupBy { it.categoryId }
        val s = MoneyCalculator.summary(accounts, goals, txs)
        val txCount = HashMap<String, Int>()
        txs.forEach { t -> txCount.merge(t.accountId, 1, Int::plus); t.toAccountId?.let { txCount.merge(it, 1, Int::plus) } }
        ManageState(
            cats.map { cat -> byCat[cat.id].orEmpty().let { CategoryItem(cat, it.size, it.sumOf { t -> t.amountCents }) } },
            accounts.map { AccountItem(it, s.account(it.id), txCount[it.id] ?: 0) },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageState())

    fun addCategory(name: String, icon: String, color: Long, budget: Long? = null) =
        viewModelScope.launch { if (name.isNotBlank()) c.categories.add(name, icon, color, budget) }
    fun saveCategory(cat: Category) = viewModelScope.launch { if (cat.name.isNotBlank()) c.categories.save(cat.copy(name = cat.name.trim())) }
    fun reorderCategories(ids: List<String>) = viewModelScope.launch { c.categories.reorder(ids) }
    fun setCategoryArchived(id: String, archived: Boolean) = viewModelScope.launch { c.categories.setArchived(id, archived) }

    fun deleteCategory(cat: Category, done: (CategoryDelete) -> Unit) = viewModelScope.launch {
        val deleted = c.categories.deleteIfUnused(cat.id)
        done(if (deleted != null) CategoryDelete.Deleted(deleted) else CategoryDelete.InUse(cat, c.categories.expenseCount(cat.id)))
    }

    fun moveAndDelete(cat: Category, targetId: String, done: (MovedCategory?) -> Unit) = viewModelScope.launch {
        done(if (targetId == cat.id) null else c.categories.moveAndDelete(cat.id, targetId))
    }

    // Undo runs on the app scope: the screen (and this VM) may be gone when the snackbar action fires.
    fun undoCategoryDelete(cat: Category) = c.appScope.launch { c.categories.restore(cat) }
    fun undoMove(m: MovedCategory) = c.appScope.launch { c.categories.undoMove(m) }

    fun addAccount(name: String, icon: String) = viewModelScope.launch { if (name.isNotBlank()) c.accounts.add(name, icon) }
    fun saveAccount(a: Account) = viewModelScope.launch { if (a.name.isNotBlank()) c.accounts.save(a.copy(name = a.name.trim())) }
    fun reorderAccounts(ids: List<String>) = viewModelScope.launch { c.accounts.reorder(ids) }

    fun setAccountArchived(item: AccountItem, archived: Boolean, done: (String?) -> Unit) = viewModelScope.launch {
        if (archived && item.money.allocated != 0L) return@launch done(c.context.getString(R.string.manage_account_has_goal_money))
        c.accounts.setArchived(item.account.id, archived); done(null)
    }

    fun deleteAccount(id: String, done: (AccountDeleteResult) -> Unit) = viewModelScope.launch { done(c.accounts.delete(id)) }
    fun undoAccountDelete(a: Account) = c.appScope.launch { c.accounts.restore(a) }

    fun resetAll(done: () -> Unit) = viewModelScope.launch { c.backup.resetAll(); done() }
}
