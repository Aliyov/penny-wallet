package com.penny.wallet.feature.expenses

import com.penny.wallet.R
import androidx.compose.ui.res.stringResource
import com.penny.wallet.core.util.dateText
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.feature.home.pennyFieldColors
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.clickableNoRipple
import com.penny.wallet.core.ui.theme.Penny
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun ExpensesHeader(onOpenRecurring: () -> Unit, onOpenSearch: () -> Unit, onOpenYear: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val shape = RoundedCornerShape(50)
    Row(modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.expenses_title), style = Penny.type.amountLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
        IconButton(onClick = onOpenYear) { Icon(Icons.Rounded.BarChart, stringResource(R.string.year_title), tint = c.textPrimary) }
        IconButton(onClick = onOpenSearch) { Icon(Icons.Rounded.Search, stringResource(R.string.search_open), tint = c.textPrimary) }
        Row(
            Modifier.clip(shape).background(c.surface1).border(0.75.dp, c.hairline, shape)
                .clickableNoRipple(onOpenRecurring).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Repeat, null, tint = c.accent, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.expenses_recurring), style = Penny.type.label, color = c.textPrimary)
        }
    }
}

/** Last-N-months bars: selected month in accent, others dim; tap a bar to jump to that month. */
@Composable
internal fun HistoryChart(bars: List<MonthBar>, onSelect: (YearMonth) -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val max = (bars.maxOfOrNull { it.amount } ?: 0L).coerceAtLeast(1L)
    PennyCard(modifier.fillMaxWidth()) {
        val sel = bars.firstOrNull { it.selected }
        if (sel != null) {
            Text(dateText().month(sel.label), style = Penny.type.label, color = c.textSecondary)
            MoneyText(sel.amount, Penny.type.amount, c.textPrimary)
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            bars.forEach { b ->
                val fraction by animateFloatAsState(b.amount.toFloat() / max, Penny.motion.spec(), label = "hist")
                val color by animateColorAsState(if (b.selected) c.accent else c.textSecondary.copy(alpha = 0.3f), label = "histc")
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickableNoRipple { onSelect(b.label) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.fillMaxWidth(0.7f).fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(color),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(dateText().monthShort(b.label), style = Penny.type.caption, color = if (b.selected) c.textPrimary else c.textSecondary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String? = null) {
    val c = Penny.colors
    OutlinedTextField(
        query, onChange, modifier.fillMaxWidth(),
        placeholder = { Text(placeholder ?: stringResource(R.string.expenses_search_hint), color = c.textSecondary) },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.textSecondary) },
        trailingIcon = if (query.isNotEmpty()) {
            { Icon(Icons.Rounded.Close, stringResource(R.string.expenses_search_clear), tint = c.textSecondary, modifier = Modifier.clickableNoRipple { onChange("") }) }
        } else null,
        singleLine = true, shape = Penny.dims.smallShape, colors = pennyFieldColors(), textStyle = Penny.type.body,
    )
}

@Composable
internal fun CategoryFilterChip(name: String, color: Color, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.clip(shape).background(color.copy(alpha = 0.16f)).border(0.75.dp, color, shape)
            .clickableNoRipple(onClear).padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(name, style = Penny.type.label, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.Rounded.Close, stringResource(R.string.expenses_filter_clear), tint = color, modifier = Modifier.size(14.dp))
    }
}

@Composable
internal fun DayHeader(date: LocalDate, total: Long, modifier: Modifier = Modifier) {
    val c = Penny.colors
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(dateText().day(date).uppercase(), style = Penny.type.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
        MoneyText(total, Penny.type.caption, c.textSecondary)
    }
}
