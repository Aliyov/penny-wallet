package com.penny.wallet.core.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.domain.MoneyCalculator

/** Display model for one ledger row. Built in ViewModels; only the final text is resolved in the composable. */
data class TxRow(
    val tx: Transaction,
    val title: RowText,
    val subtitle: RowText,
    val icon: String,
    /** ARGB, or null to use the theme accent / secondary. */
    val color: Long?,
    /** Signed amount from the viewer's perspective; for neutral moves (transfer, save) it's positive. */
    val amount: Long,
    val tone: Tone,
) {
    enum class Tone { NEGATIVE, POSITIVE, NEUTRAL }
}

/** Row text that is either user data or a string resource, so ViewModels stay free of UI strings. */
sealed interface RowText {
    data class Plain(val text: String) : RowText
    data class Res(@StringRes val id: Int, val args: List<RowText> = emptyList()) : RowText
}

@Composable
fun RowText.resolve(): String = when (this) {
    is RowText.Plain -> text
    is RowText.Res -> if (args.isEmpty()) stringResource(id) else stringResource(id, *args.map { it.resolve() }.toTypedArray())
}

object TxRows {
    /** [perspective]: account id when viewing one account (transfers become ±), null for global lists. */
    fun build(
        txs: List<Transaction>, accounts: List<Account>, categories: List<Category>, goals: List<Goal>,
        perspective: String? = null,
    ): List<TxRow> {
        val acc = accounts.associateBy { it.id }
        val cat = categories.associateBy { it.id }
        val goal = goals.associateBy { it.id }
        fun an(id: String?): RowText =
            if (id == MoneyCalculator.EXTERNAL) RowText.Res(R.string.ui_tx_outside) else RowText.Plain(id?.let { acc[it]?.name } ?: "—")
        fun text(s: String?, @StringRes fallback: Int): RowText = s?.let(RowText::Plain) ?: RowText.Res(fallback)
        return txs.map { t ->
            val note = t.note?.takeIf { it.isNotBlank() }
            when (t.type) {
                TxType.EXPENSE -> {
                    val c = t.categoryId?.let(cat::get)
                    val sub = if (note != null && c != null) RowText.Res(R.string.ui_tx_pair, listOf(RowText.Plain(c.name), an(t.accountId))) else an(t.accountId)
                    TxRow(t, text(note ?: c?.name, R.string.type_expense), sub, c?.icon ?: "other", c?.color, -t.amountCents, TxRow.Tone.NEGATIVE)
                }
                TxType.INCOME -> TxRow(t, text(note, R.string.type_income), an(t.accountId), "salary", null, t.amountCents, TxRow.Tone.POSITIVE)
                TxType.WITHDRAWAL -> TxRow(t, text(note, R.string.ui_tx_withdrawal), an(t.accountId), "cash", null, -t.amountCents, TxRow.Tone.NEGATIVE)
                TxType.ADJUSTMENT -> TxRow(t, RowText.Res(R.string.ui_tx_balance_adjusted), an(t.accountId), "tools", null, t.amountCents,
                    if (t.amountCents < 0) TxRow.Tone.NEGATIVE else TxRow.Tone.POSITIVE)
                TxType.TRANSFER -> {
                    val signed = if (perspective == null) t.amountCents else MoneyCalculator.effectOn(t, perspective)
                    val tone = when { perspective == null -> TxRow.Tone.NEUTRAL; signed < 0 -> TxRow.Tone.NEGATIVE; else -> TxRow.Tone.POSITIVE }
                    TxRow(t, text(note, R.string.type_transfer), RowText.Res(R.string.ui_tx_route, listOf(an(t.accountId), an(t.toAccountId))),
                        "transfer", null, signed, tone)
                }
                TxType.SAVE, TxType.UNSAVE -> {
                    val g = t.goalId?.let(goal::get)
                    val save = t.type == TxType.SAVE
                    val goalName = text(g?.name, R.string.ui_tx_goal)
                    TxRow(t, RowText.Res(if (save) R.string.ui_tx_saved_to else R.string.ui_tx_taken_from, listOf(goalName)),
                        RowText.Res(if (save) R.string.ui_tx_from_account else R.string.ui_tx_to_account, listOf(an(t.accountId))),
                        g?.icon ?: "savings", g?.color, t.amountCents, TxRow.Tone.NEUTRAL)
                }
            }
        }
    }
}
