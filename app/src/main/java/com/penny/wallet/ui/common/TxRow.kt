package com.penny.wallet.ui.common

import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.domain.MoneyCalculator

/** Display model for one ledger row. Built in ViewModels, never in composables. */
data class TxRow(
    val tx: Transaction,
    val title: String,
    val subtitle: String,
    val icon: String,
    /** ARGB, or null to use the theme accent / secondary. */
    val color: Long?,
    /** Signed amount from the viewer's perspective; for neutral moves (transfer, save) it's positive. */
    val amount: Long,
    val tone: Tone,
) {
    enum class Tone { NEGATIVE, POSITIVE, NEUTRAL }
}

object TxRows {
    /**
     * @param perspective account id when viewing one account (transfers become ±), null for global lists.
     */
    fun build(
        txs: List<Transaction>, accounts: List<Account>, categories: List<Category>, goals: List<Goal>,
        perspective: String? = null,
    ): List<TxRow> {
        val acc = accounts.associateBy { it.id }
        val cat = categories.associateBy { it.id }
        val goal = goals.associateBy { it.id }
        fun an(id: String?) = if (id == MoneyCalculator.EXTERNAL) "outside" else id?.let { acc[it]?.name } ?: "—"
        return txs.map { t ->
            val note = t.note?.takeIf { it.isNotBlank() }
            when (t.type) {
                TxType.EXPENSE -> {
                    val c = t.categoryId?.let(cat::get)
                    TxRow(t, note ?: c?.name ?: "Expense", listOfNotNull(c?.name?.takeIf { note != null }, an(t.accountId)).joinToString(" · "),
                        c?.icon ?: "other", c?.color, -t.amountCents, TxRow.Tone.NEGATIVE)
                }
                TxType.INCOME -> TxRow(t, note ?: "Income", an(t.accountId), "salary", null, t.amountCents, TxRow.Tone.POSITIVE)
                TxType.WITHDRAWAL -> TxRow(t, note ?: "Withdrawal", an(t.accountId), "cash", null, -t.amountCents, TxRow.Tone.NEGATIVE)
                TxType.ADJUSTMENT -> TxRow(t, "Balance adjusted", an(t.accountId), "tools", null, t.amountCents,
                    if (t.amountCents < 0) TxRow.Tone.NEGATIVE else TxRow.Tone.POSITIVE)
                TxType.TRANSFER -> {
                    val signed = if (perspective == null) t.amountCents else MoneyCalculator.effectOn(t, perspective)
                    val tone = when { perspective == null -> TxRow.Tone.NEUTRAL; signed < 0 -> TxRow.Tone.NEGATIVE; else -> TxRow.Tone.POSITIVE }
                    TxRow(t, note ?: "Transfer", "${an(t.accountId)} → ${an(t.toAccountId)}", "transfer", null, signed, tone)
                }
                TxType.SAVE, TxType.UNSAVE -> {
                    val g = t.goalId?.let(goal::get)
                    val save = t.type == TxType.SAVE
                    TxRow(t, (if (save) "Saved to " else "Taken from ") + (g?.name ?: "goal"),
                        if (save) "from ${an(t.accountId)}" else "to ${an(t.accountId)}",
                        g?.icon ?: "savings", g?.color, t.amountCents, TxRow.Tone.NEUTRAL)
                }
            }
        }
    }
}
