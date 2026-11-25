package com.penny.wallet.feature.year

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.clickableNoRipple
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.currentLocale
import com.penny.wallet.domain.YearOverview
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
private fun monthName(ym: YearMonth): String = ym.month.getDisplayName(TextStyle.FULL, currentLocale())

@Composable
internal fun TotalsCard(o: YearOverview, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val net = o.income - o.spent
    PennyCard(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TotalLine(stringResource(R.string.year_spent), o.spent, c.textPrimary)
            TotalLine(stringResource(R.string.year_income), o.income, c.accent)
            TotalLine(stringResource(R.string.year_saved), o.saved, c.textPrimary)
            HorizontalDivider(color = c.hairline)
            TotalLine(
                stringResource(R.string.year_net), net,
                when { net > 0 -> c.accent; net < 0 -> c.danger; else -> c.textPrimary }, signed = true,
            )
            if (o.averageMonthlySpent > 0) {
                Text(stringResource(R.string.year_average, money(o.averageMonthlySpent)), style = Penny.type.caption, color = c.textSecondary)
            }
            o.biggestMonth?.let { b ->
                Text(stringResource(R.string.year_most, monthName(b.label)), style = Penny.type.caption, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun TotalLine(label: String, cents: Long, color: Color, signed: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Penny.type.body, color = Penny.colors.textSecondary, modifier = Modifier.weight(1f))
        MoneyText(cents, Penny.type.amount, color, signed = signed)
    }
}

/** Twelve narrow bars, so labels use the one-letter month names; the biggest month is in accent. */
@Composable
internal fun YearChart(o: YearOverview, onOpenMonth: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val locale = currentLocale()
    val max = (o.months.maxOfOrNull { it.spent } ?: 0L).coerceAtLeast(1L)
    PennyCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            o.months.forEach { m ->
                val top = m.label == o.biggestMonth?.label
                val fraction by animateFloatAsState(m.spent.toFloat() / max, Penny.motion.spec(), label = "yearbar")
                val a11y = stringResource(R.string.year_bar, monthName(m.label), money(m.spent))
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickableNoRipple { onOpenMonth(m.label) }
                        .clearAndSetSemantics { contentDescription = a11y; role = Role.Button },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        if (m.spent > 0) Box(
                            Modifier.fillMaxWidth(0.75f).fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(if (top) c.accent else c.textSecondary.copy(alpha = 0.3f)),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        m.label.month.getDisplayName(TextStyle.NARROW_STANDALONE, locale), style = Penny.type.caption,
                        color = if (top) c.textPrimary else c.textSecondary, maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MonthList(o: YearOverview, onOpenMonth: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val locale = currentLocale()
    val months = o.months.filter { it.spent != 0L || it.income != 0L }
    if (months.isEmpty()) return
    PennyCard(modifier.fillMaxWidth()) {
        Column {
            months.forEachIndexed { i, m ->
                if (i > 0) HorizontalDivider(color = c.hairline)
                Row(
                    Modifier.fillMaxWidth().clickableNoRipple { onOpenMonth(m.label) }.semantics { role = Role.Button }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        monthName(m.label).replaceFirstChar { it.titlecase(locale) }, style = Penny.type.bodyStrong, color = c.textPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        MoneyText(m.spent, Penny.type.amount, c.textPrimary)
                        if (m.income != 0L) MoneyText(m.income, Penny.type.caption, c.accent, signed = true)
                    }
                }
            }
        }
    }
}
