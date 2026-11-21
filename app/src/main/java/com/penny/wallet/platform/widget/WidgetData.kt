package com.penny.wallet.platform.widget

import android.content.Context
import com.penny.wallet.R
import com.penny.wallet.container
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.util.DateText
import com.penny.wallet.data.prefs.BackgroundStyle
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.domain.Recurrence
import java.time.LocalDate

/** Pre-formatted, privacy mode applied, computed from the ledger like the app. */
data class WidgetData(
    val available: String,
    val negative: Boolean,
    val nextAmount: String?,
    val accentArgb: Long,
    val pureBlack: Boolean,
    val labels: WidgetLabels,
)

data class WidgetLabels(val available: String, val spentMonth: String, val next: String?, val addExpense: String)

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
    val dates = DateText.of(context)
    val f = Formatters.of(p, dates.locale)
    fun m(v: Long) = if (p.hideAmounts) Formatters.HIDDEN else Formatters.money(v, f)
    return WidgetData(
        available = m(s.availableToSpend), negative = s.availableToSpend < 0,
        nextAmount = next?.let { m(it.amountCents) },
        accentArgb = p.accentArgb, pureBlack = p.background == BackgroundStyle.AMOLED,
        labels = WidgetLabels(
            available = context.getString(R.string.widget_available),
            spentMonth = context.getString(R.string.widget_spent_month, m(spent)),
            next = next?.let { context.getString(R.string.widget_next, it.name, dates.day(it.nextDueDate, today)) },
            addExpense = context.getString(R.string.widget_add_expense),
        ),
    )
}
