package com.penny.wallet.feature.debts

import com.penny.wallet.platform.notify.Notifications
import com.penny.wallet.data.repo.DeletedPayment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.core.ui.userMessage
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.DebtPayment
import com.penny.wallet.data.repo.DeletedDebt
import com.penny.wallet.domain.DebtStatus
import com.penny.wallet.domain.DebtTotals
import com.penny.wallet.domain.Debts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DebtsState(
    val loading: Boolean = true,
    val statuses: List<DebtStatus> = emptyList(),
    val totals: DebtTotals = DebtTotals(0, 0, 0),
    val paymentsByDebt: Map<String, List<DebtPayment>> = emptyMap(),
    val accounts: List<Account> = emptyList(),
)

class DebtsViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<DebtsState> = combine(c.debts.observe(), c.debts.observePayments(), c.accounts.observe()) { debts, payments, accounts ->
        val statuses = Debts.statuses(debts, payments, LocalDate.now())
        DebtsState(
            loading = false,
            statuses = statuses,
            totals = Debts.totals(statuses),
            paymentsByDebt = payments.groupBy { it.debtId }.mapValues { (_, list) -> list.sortedWith(compareByDescending<DebtPayment> { it.date }.thenByDescending { it.createdAt }) },
            accounts = accounts.filterNot { it.archived },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtsState())

    fun save(
        editing: Debt?, person: String, direction: DebtDirection, amount: Long, dueDate: LocalDate?, remind: Boolean, note: String,
        onDone: () -> Unit, onError: (String) -> Unit,
    ) = run(onDone, onError) {
        val cleanNote = note.trim().takeIf { it.isNotEmpty() }
        if (editing == null) c.debts.add(person, direction, amount, LocalDate.now(), dueDate, remind, cleanNote)
        else c.debts.update(editing.copy(person = person, direction = direction, amountCents = amount, dueDate = dueDate, remind = remind, note = cleanNote))
    }

    fun addPayment(debtId: String, amount: Long, recordInAccount: String?, onDone: () -> Unit, onError: (String) -> Unit) =
        run(onDone, onError) { c.debts.addPayment(debtId, amount, LocalDate.now(), recordInAccount = recordInAccount) }

    fun deletePayment(p: DebtPayment, onDeleted: (DeletedPayment) -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { c.debts.deletePayment(p) }.onSuccess { it?.let(onDeleted) }.onFailure { onError(c.userMessage(it)) }
    }

    fun setSettled(id: String, settled: Boolean, onError: (String) -> Unit) = run({ if (settled) cancelReminder(id) }, onError) { c.debts.setSettled(id, settled) }

    fun delete(id: String, onDeleted: (DeletedDebt) -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        runCatching { c.debts.delete(id) }.onSuccess { it?.let { gone -> cancelReminder(id); onDeleted(gone) } }.onFailure { onError(c.userMessage(it)) }
    }

    // App scope: the Undo snackbar outlives this screen, and a lost undo would lose recorded account transactions too.
    fun restore(d: DeletedDebt) = c.appScope.launch { c.debts.restore(d) }

    fun restorePayment(d: DeletedPayment) = c.appScope.launch { c.debts.restorePayment(d) }

    private fun cancelReminder(id: String) = Notifications.cancel(c.context, "debt-$id")

    private fun run(onDone: () -> Unit, onError: (String) -> Unit, block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onSuccess { onDone() }.onFailure { onError(c.userMessage(it)) }
    }
}
