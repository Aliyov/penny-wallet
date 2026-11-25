package com.penny.wallet.platform.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.penny.wallet.MainActivity
import com.penny.wallet.R
import com.penny.wallet.container
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.util.DateText
import com.penny.wallet.data.prefs.BackgroundStyle
import com.penny.wallet.domain.Budget
import com.penny.wallet.domain.BudgetPace
import com.penny.wallet.domain.BudgetStatus
import com.penny.wallet.domain.MoneyCalculator
import java.time.LocalDate

/** Pre-formatted texts; [fraction] null hides the bar, [pace] null keeps the neutral accent colour. */
data class BudgetWidgetData(
    val title: String,
    val amount: String?,
    val detail: String?,
    val fraction: Float?,
    val pace: BudgetPace?,
    val accentArgb: Long,
    val pureBlack: Boolean,
)

/** Pure so every budget state can be tested without a widget host. */
internal fun budgetWidgetData(
    status: BudgetStatus?, money: (Long) -> String, text: (Int, Array<out Any>) -> String, accentArgb: Long, pureBlack: Boolean,
    hideAmounts: Boolean = false,
): BudgetWidgetData {
    if (status == null) {
        return BudgetWidgetData(text(R.string.widget_budget_none, emptyArray()), null, text(R.string.widget_budget_set, emptyArray()), null, null, accentArgb, pureBlack)
    }
    // Privacy mode: the bar, the colour and "over budget" would tell an onlooker as much as the numbers.
    if (hideAmounts) {
        return BudgetWidgetData(text(R.string.widget_budget_title, emptyArray()), money(0), null, null, null, accentArgb, pureBlack)
    }
    val over = status.left < 0
    return BudgetWidgetData(
        title = text(if (over) R.string.widget_budget_over else R.string.widget_budget_left, emptyArray()),
        amount = money(if (over) -status.left else status.left),
        detail = if (over) text(R.string.widget_budget_of, arrayOf(money(status.spent), money(status.budget)))
        else text(R.string.widget_budget_per_day, arrayOf(money(status.perDay))),
        fraction = status.fraction, pace = status.pace, accentArgb = accentArgb, pureBlack = pureBlack,
    )
}

suspend fun loadBudgetWidgetData(context: Context): BudgetWidgetData {
    val c = context.container
    val p = c.settings.current()
    val today = LocalDate.now()
    val period = MoneyCalculator.periodOf(today, p.monthStartDay)
    val spent = MoneyCalculator.monthSpent(c.transactions.all(), period)
    val f = Formatters.of(p, DateText.of(context).locale)
    return budgetWidgetData(
        Budget.status(p.monthlyBudget, spent, period, today),
        money = { if (p.hideAmounts) Formatters.HIDDEN else Formatters.money(it, f) },
        text = { id, args -> context.getString(id, *args) },
        accentArgb = p.accentArgb, pureBlack = p.background == BackgroundStyle.AMOLED, hideAmounts = p.hideAmounts,
    )
}

class BudgetWidget : GlanceAppWidget() {
    companion object {
        // One launcher row is rarely under 56dp; at that size only the amount and the bar fit.
        private val SMALL = DpSize(110.dp, 56.dp)
        private val LARGE = DpSize(180.dp, 100.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = loadBudgetWidgetData(context)
        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        provideContent { Content(d, open) }
    }

    @Composable
    private fun Content(d: BudgetWidgetData, open: Intent) {
        val large = LocalSize.current.height >= LARGE.height
        val wc = WidgetColors(d.accentArgb, d.pureBlack)
        val tone = when (d.pace) {
            BudgetPace.OVER -> wc.danger
            BudgetPace.FAST -> wc.warning
            else -> wc.accent
        }
        val amountOnly = !large && d.amount != null && d.fraction != null
        Column(
            GlanceModifier.fillMaxSize().background(wc.background).cornerRadius(22.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp).clickable(actionStartActivity(open)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!amountOnly) Text(d.title, maxLines = 1, style = TextStyle(color = wc.secondary, fontSize = 11.sp))
            d.amount?.let {
                Text(it, maxLines = 1, style = TextStyle(color = tone, fontSize = if (large) 28.sp else 20.sp, fontWeight = FontWeight.Bold))
            }
            d.fraction?.let {
                Spacer(GlanceModifier.height(6.dp))
                LinearProgressIndicator(it, GlanceModifier.fillMaxWidth().height(6.dp), color = tone, backgroundColor = wc.track)
            }
            if (d.detail != null && (large || d.amount == null)) {
                Spacer(GlanceModifier.height(6.dp))
                Text(d.detail, maxLines = 1, style = TextStyle(color = wc.secondary, fontSize = 12.sp))
            }
        }
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}
