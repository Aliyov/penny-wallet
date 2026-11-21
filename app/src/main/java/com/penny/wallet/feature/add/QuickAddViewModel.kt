package com.penny.wallet.feature.add

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.prefs.ChipOrder
import com.penny.wallet.data.prefs.QuickType
import com.penny.wallet.data.repo.InsufficientAvailableException
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.domain.Recurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Editable form state. Amount is a numpad digit string (cents). */
data class QuickForm(
    val type: QuickType = QuickType.EXPENSE,
    val digits: String = "",
    val accountId: String? = null,
    val toAccountId: String? = null,
    val goalId: String? = null,
    val categoryId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val recurring: Boolean = false,
    /** Non-null when editing an existing transaction. */
    val editing: Transaction? = null,
) {
    val cents: Long get() = Formatters.digitsToCents(digits)
}

data class QuickData(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val goals: List<Goal> = emptyList(),
    /** Available (not earmarked) per account, for the Save hint. */
    val available: Map<String, Long> = emptyMap(),
    val saveOnChipTap: Boolean = true,
    val showNote: Boolean = true,
)

sealed interface QuickResult {
    data class Saved(val message: String, val undo: Transaction?, val askNotificationPermission: Boolean) : QuickResult
    data class Error(val message: String) : QuickResult
}

class QuickAddViewModel(private val c: AppContainer) : ViewModel() {
    private val _form = MutableStateFlow(QuickForm())
    val form: StateFlow<QuickForm> = _form

    val data: StateFlow<QuickData> = combine(
        c.accounts.observe(), c.categories.observe(), c.goals.observe(), c.transactions.observe(), c.settings.prefs,
    ) { accounts, cats, goals, txs, prefs ->
        val live = accounts.filterNot { it.archived }
        val usage = txs.asSequence().filter { it.type == TxType.EXPENSE && it.categoryId != null }
            .groupingBy { it.categoryId!! }.eachCount()
        val visible = cats.filterNot { it.archived }
        val ordered = when (prefs.chipOrder) {
            ChipOrder.MANUAL -> visible.sortedBy { it.sortOrder }
            ChipOrder.ALPHABETICAL -> visible.sortedBy { it.name.lowercase() }
            ChipOrder.MOST_USED -> visible.sortedWith(compareByDescending<Category> { usage[it.id] ?: 0 }.thenBy { it.sortOrder })
        }.let { list -> // last used first (SPEC §8)
            val last = prefs.lastCategoryId
            if (prefs.chipOrder == ChipOrder.MOST_USED && last != null) list.sortedByDescending { it.id == last } else list
        }
        val summary = MoneyCalculator.summary(accounts, goals, txs)
        QuickData(live, ordered, goals.filterNot { it.archived }, summary.perAccount.mapValues { it.value.available },
            prefs.saveOnChipTap, prefs.showNoteField)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickData())

    /** Resets the form for a new entry with defaults from settings (account: fixed or last used). */
    fun startNew(type: QuickType? = null) = viewModelScope.launch {
        val p = c.settings.current()
        val accounts = c.accounts.all().filterNot { it.archived }
        val acc = listOfNotNull(p.fixedAccountId, p.lastAccountId).firstOrNull { id -> accounts.any { it.id == id } } ?: accounts.firstOrNull()?.id
        _form.value = QuickForm(type = type ?: p.quickAddDefaultType, accountId = acc,
            toAccountId = accounts.firstOrNull { it.id != acc }?.id, goalId = c.goals.all().firstOrNull { !it.archived }?.id)
    }

    fun startEdit(txId: String) = viewModelScope.launch {
        val t = c.transactions.get(txId) ?: return@launch
        val type = when (t.type) {
            TxType.EXPENSE -> QuickType.EXPENSE; TxType.INCOME -> QuickType.INCOME
            TxType.TRANSFER -> QuickType.TRANSFER; TxType.SAVE -> QuickType.SAVE
            else -> null // withdrawal/adjustment/unsave: amount, date, note only
        }
        _form.value = QuickForm(type ?: QuickType.EXPENSE, NumpadDigits.of(kotlin.math.abs(t.amountCents)), t.accountId, t.toAccountId,
            t.goalId, t.categoryId, t.date, t.note.orEmpty(), editing = t)
    }

    fun edit(f: (QuickForm) -> QuickForm) = _form.update(f)

    // ---- Receipt scan: photo → on-device OCR → confirm dialog → amount ----
    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan: StateFlow<ScanState> = _scan
    private var scanJob: kotlinx.coroutines.Job? = null

    fun newReceiptPhotoUri(): android.net.Uri = c.receipts.newPhotoUri()

    fun readReceipt(uri: android.net.Uri) {
        scanJob?.cancel()
        _scan.value = ScanState.Reading
        scanJob = viewModelScope.launch {
            _scan.value = runCatching { c.receipts.read(uri).inMinorUnits(Formatters.fractionDigits(c.settings.current().currencyCode)) }.fold(
                { r -> if (r.totalCents == null) ScanState.NothingFound else ScanState.Found(r) },
                { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    android.util.Log.w("PennyReceipt", "Receipt scan failed", e)
                    ScanState.Failed("Couldn't read this photo")
                },
            )
            c.receipts.discardPhoto()
        }
    }

    /** Accepted amount goes into the form; the user still picks a category, then it saves as usual. */
    fun acceptReceipt(cents: Long) {
        if (cents > 0) _form.update { it.copy(digits = NumpadDigits.of(cents), type = QuickType.EXPENSE) }
        closeScan()
    }

    fun scanFailed(message: String) {
        _scan.value = ScanState.Failed(message)
    }

    fun closeScan() {
        scanJob?.cancel()
        scanJob = null
        _scan.value = ScanState.Idle
        c.receipts.discardPhoto()
    }

    override fun onCleared() {
        c.receipts.discardPhoto()
    }

    /** Category chip tap: selects it, and saves immediately when that's enabled and an amount is entered. */
    fun onCategory(id: String, done: (QuickResult) -> Unit) {
        _form.update { it.copy(categoryId = id) }
        if (data.value.saveOnChipTap && form.value.cents > 0 && form.value.editing == null) save(done)
    }

    fun save(done: (QuickResult) -> Unit) = viewModelScope.launch {
        val f = form.value
        val r = runCatching { if (f.editing != null) saveEdit(f) else saveNew(f) }
            .getOrElse { e ->
                QuickResult.Error(if (e is InsufficientAvailableException) "Only ${Formatters.money(e.availableCents)} available" else e.message ?: "Couldn't save")
            }
        done(r)
    }

    private suspend fun saveNew(f: QuickForm): QuickResult {
        val acc = requireNotNull(f.accountId) { "Add an account first" }
        val amt = f.cents
        require(amt > 0) { "Enter an amount" }
        val note = f.note.takeIf { it.isNotBlank() }
        val t = when (f.type) {
            QuickType.EXPENSE -> c.transactions.addExpense(amt, acc, requireNotNull(f.categoryId) { "Pick a category" }, f.date, note)
            QuickType.INCOME -> c.transactions.addIncome(amt, acc, f.date, note)
            QuickType.TRANSFER -> c.transactions.transfer(amt, acc, requireNotNull(f.toAccountId) { "Pick where to" }, f.date, note)
            QuickType.SAVE -> c.transactions.save(amt, acc, requireNotNull(f.goalId) { "Pick a goal" }, f.date)
        }
        var askPermission = false
        if (f.recurring && f.type == QuickType.EXPENSE) {
            askPermission = makeRecurring(t, note)
        }
        c.settings.update { it.copy(lastAccountId = acc, lastCategoryId = f.categoryId ?: it.lastCategoryId) }
        val label = when (f.type) { QuickType.EXPENSE -> "Expense"; QuickType.INCOME -> "Income"; QuickType.TRANSFER -> "Transfer"; QuickType.SAVE -> "Saved" }
        // re-read: makeRecurring sets recurringId, which Undo needs to remove the rule too
        return QuickResult.Saved("$label · ${Formatters.money(amt)}", c.transactions.get(t.id) ?: t, askPermission)
    }

    /** Creates a monthly rule anchored on the expense's day, already paid for this occurrence. */
    private suspend fun makeRecurring(t: Transaction, note: String?): Boolean {
        val cat = c.categories.all().firstOrNull { it.id == t.categoryId }
        val p = c.settings.current()
        val id = UUID.randomUUID().toString()
        val base = RecurringExpense(id, note ?: cat?.name ?: "Recurring", t.amountCents, t.categoryId!!, t.accountId,
            Frequency.MONTHLY, dayOfMonth = t.date.dayOfMonth, nextDueDate = t.date, remindDaysBefore = p.defaultRemindDaysBefore)
        c.recurring.save(base.copy(nextDueDate = Recurrence.next(base, t.date)))
        c.transactions.update(t.copy(recurringId = id))
        if (!p.notificationPermissionAsked) { c.settings.update { it.copy(notificationPermissionAsked = true) }; return true }
        return false
    }

    private suspend fun saveEdit(f: QuickForm): QuickResult {
        val old = f.editing!!
        val amt = f.cents
        require(amt > 0) { "Enter an amount" }
        val acc = f.accountId ?: old.accountId
        val updated = when (old.type) {
            TxType.EXPENSE -> old.copy(amountCents = amt, accountId = acc, categoryId = f.categoryId ?: old.categoryId)
            TxType.TRANSFER -> {
                val to = f.toAccountId ?: old.toAccountId
                require(to != acc) { "Pick two different accounts" }
                old.copy(amountCents = amt, accountId = acc, toAccountId = to)
            }
            TxType.SAVE -> {
                // available excluding this row's own earmark
                val others = c.transactions.all().filter { it.id != old.id }
                val avail = MoneyCalculator.availableInAccount(acc, others)
                if (amt > avail) throw InsufficientAvailableException(avail)
                old.copy(amountCents = amt, accountId = acc, goalId = f.goalId ?: old.goalId)
            }
            TxType.ADJUSTMENT -> old.copy(amountCents = if (old.amountCents < 0) -amt else amt)
            else -> old.copy(amountCents = amt, accountId = acc)
        }.copy(date = f.date, note = f.note.takeIf { it.isNotBlank() })
        c.transactions.update(updated)
        return QuickResult.Saved("Updated", null, false)
    }

    fun deleteEditing(done: (Transaction?) -> Unit) = viewModelScope.launch {
        val t = form.value.editing ?: return@launch done(null)
        c.transactions.delete(t); done(t)
    }

    fun undoNew(t: Transaction) = viewModelScope.launch {
        c.transactions.delete(t)
        t.recurringId?.let { id -> c.recurring.get(id)?.let { c.recurring.delete(it) } }
    }

    fun restore(t: Transaction) = viewModelScope.launch { c.transactions.restore(t) }
}

object NumpadDigits { fun of(cents: Long) = if (cents <= 0) "" else cents.toString() }
