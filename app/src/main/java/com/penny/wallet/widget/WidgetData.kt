package com.penny.wallet.widget

import android.content.Context
import com.penny.wallet.container
import com.penny.wallet.domain.Formatters
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.domain.Recurrence
import java.time.LocalDate

/** Everything the widgets show, pre-formatted (privacy mode applied). Computed from the ledger like the app. */
data class WidgetData(
    val available: String,
    val negative: Boolean,
    val spentMonth: String,
    val nextName: String?,
    val nextWhen: String?,
    val nextAmount: String?,
    val accentArgb: Long,
    val darkGrey: Boolean,
)

suspend fun loadWidgetData(context: Context): WidgetData {
    val c = context.container
    val p = c.settings.current()
    val accounts = c.accounts.all()
    val goals = c.goals.all()
    val txs = c.transactions.all()
    val today = LocalDate.now()
    val s = MoneyCalculator.summary(accounts, goals, txs)
    val spent = MoneyCalculator.monthSpent(txs, MoneyCalculator.periodOf(today, p.monthStartDay))
    val next = Recurrence.upcoming(c.recurring.all(), today, days = 31).firstOrNull()
    val f = Formatters.of(p)
    fun m(v: Long) = if (p.hideAmounts) Formatters.HIDDEN else Formatters.money(v, f)
    return WidgetData(
        available = m(s.availableToSpend), negative = s.availableToSpend < 0, spentMonth = m(spent),
        nextName = next?.name, nextWhen = next?.let { Formatters.day(it.nextDueDate, today) }, nextAmount = next?.let { m(it.amountCents) },
        accentArgb = p.accentArgb, darkGrey = p.background == com.penny.wallet.data.prefs.BackgroundStyle.DARK_GREY,
    )
}
