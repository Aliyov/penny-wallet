package com.penny.wallet.feature.expenses

import com.penny.wallet.R
import androidx.compose.ui.res.stringResource
import com.penny.wallet.core.util.currentLocale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.prefs.ChartStyle
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.util.dateText
import com.penny.wallet.core.ui.components.Donut
import com.penny.wallet.core.ui.components.DonutSlice
import com.penny.wallet.core.ui.components.IconBadge
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.clickableNoRipple
import com.penny.wallet.core.ui.theme.Penny

/** Donut or horizontal bars (per prefs.defaultChart), empty state when nothing spent. */
@Composable
internal fun ChartCard(state: ExpensesState, selected: Int?, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier) {
    PennyCard(modifier.fillMaxWidth()) {
        if (state.total == 0L || state.slices.isEmpty()) {
            EmptyChart()
        } else when (state.chart) {
            ChartStyle.DONUT -> DonutChart(state, selected, onSelect)
            else -> CategoryBars(state.slices, onTap = { i -> onSelect(if (i == selected) null else i) }, selected)
        }
    }
}

@Composable
private fun EmptyChart() {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.expenses_empty_title), style = Penny.type.title, color = Penny.colors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.expenses_empty_body), style = Penny.type.label, color = Penny.colors.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DonutChart(state: ExpensesState, selected: Int?, onSelect: (Int?) -> Unit) {
    val c = Penny.colors
    val donutSlices = state.slices.map { DonutSlice(it.share, Color(it.color)) }
    val sel = selected?.let { state.slices.getOrNull(it) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Donut(
        donutSlices, selected, onSelect,
        modifier = Modifier.fillMaxWidth(0.78f).padding(vertical = 8.dp),
    ) {
        AnimatedContent(sel, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "donutCenter") { s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 36.dp)) {
                if (s == null) {
                    MoneyText(state.total, Penny.type.amountLarge, c.textPrimary)
                    Text(stringResource(R.string.expenses_spent), style = Penny.type.label, color = c.textSecondary)
                } else {
                    Text(s.name, style = Penny.type.label, color = Color(s.color), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MoneyText(s.amount, Penny.type.amountLarge, c.textPrimary)
                    Text(Formatters.percent(s.share, currentLocale()), style = Penny.type.label, color = c.textSecondary)
                }
            }
        }
    } }
}

@Composable
private fun CategoryBars(slices: List<CategorySlice>, onTap: (Int) -> Unit, selected: Int?) {
    val c = Penny.colors
    val max = slices.maxOf { it.share }.coerceAtLeast(0.0001f)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        slices.forEachIndexed { i, s ->
            val dim = selected != null && selected != i
            val fraction by animateFloatAsState(s.share / max, Penny.motion.spec(), label = "hbar$i")
            Column(Modifier.fillMaxWidth().clickableNoRipple { onTap(i) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, style = Penny.type.label, color = if (dim) c.textSecondary else c.textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(Formatters.percent(s.share, currentLocale()), style = Penny.type.label, color = c.textSecondary)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(c.hairline)) {
                    Box(
                        Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50))
                            .background(Color(s.color).copy(alpha = if (dim) 0.3f else 1f)),
                    )
                }
            }
        }
    }
}

/** Category list row: icon, name, amount, %, thin progress bar. Tap toggles the list filter. */
@Composable
internal fun CategoryRow(slice: CategorySlice, active: Boolean, dimmed: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val color = Color(slice.color)
    val fraction by animateFloatAsState(slice.share, Penny.motion.spec(), label = "catbar")
    val shape = Penny.dims.smallShape
    Row(
        modifier.fillMaxWidth().clip(shape)
            .background(if (active) color.copy(alpha = 0.12f) else Color.Transparent)
            .clickableNoRipple(onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(slice.icon, if (dimmed) c.textSecondary else color)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(slice.name, style = Penny.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                MoneyText(slice.amount, Penny.type.amount, c.textPrimary)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(50)).background(c.hairline)) {
                    Box(Modifier.fillMaxWidth(fraction.coerceIn(0.01f, 1f)).fillMaxHeight().background(color))
                }
                Spacer(Modifier.width(10.dp))
                Text(Formatters.percent(slice.share, currentLocale()), style = Penny.type.caption, color = c.textSecondary)
            }
        }
    }
}

/** Month label with animated change; next is dimmed/disabled on the current month. */
@Composable
internal fun MonthSwitcher(state: ExpensesState, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val dates = dateText()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", style = Penny.type.title, color = c.textPrimary, modifier = Modifier.clickableNoRipple(onPrev).padding(horizontal = 16.dp, vertical = 8.dp))
        AnimatedContent(state.period?.label, modifier = Modifier.weight(1f), label = "month",
            transitionSpec = { fadeIn() togetherWith fadeOut() }) { ym ->
            Text(ym?.let(dates::month) ?: "", style = Penny.type.title, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Text(
            "›", style = Penny.type.title, color = if (state.isCurrent) c.textSecondary.copy(alpha = 0.3f) else c.textPrimary,
            modifier = (if (state.isCurrent) Modifier else Modifier.clickableNoRipple(onNext)).padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
