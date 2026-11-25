package com.penny.wallet.feature.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.R
import com.penny.wallet.core.ui.common.TxRowItem
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.dateText
import java.time.YearMonth

@Composable
fun CalendarScreen(onBack: () -> Unit, onEditTransaction: (String) -> Unit) {
    val vm = pennyViewModel { CalendarViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val c = Penny.colors
    val hPad = Modifier.padding(horizontal = Penny.dims.gutter)
    val deletedText = stringResource(R.string.deleted)

    LazyColumn(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = Penny.dims.gutter, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = c.textPrimary) }
                Text(stringResource(R.string.calendar_title), style = Penny.type.title, color = c.textPrimary)
            }
        }
        item(key = "switcher") { CalendarMonthSwitcher(state.month, vm::prevMonth, vm::nextMonth) }
        val cal = state.calendar
        if (cal == null || state.loading) return@LazyColumn
        item(key = "summary") {
            Text(
                summaryText(cal.spent, cal.income), style = Penny.type.label, color = c.textSecondary,
                textAlign = TextAlign.Center, modifier = hPad.fillMaxWidth(),
            )
        }
        // 6dp sides and 2dp gaps: seven 48dp touch targets fit a 360dp screen.
        val gridPad = Modifier.padding(horizontal = 6.dp)
        item(key = "weekdays") { WeekdayRow(cal.weeks.first().first().date.dayOfWeek, gridPad) }
        items(cal.weeks, key = { "week_" + it.first().date }) { week ->
            WeekRow(week, cal.maxDaySpent, state.today, state.selected, vm::selectDay, gridPad)
        }
        val day = state.selected ?: return@LazyColumn
        item(key = "day_title") {
            val spent = cal.weeks.flatten().firstOrNull { it.date == day }?.spent ?: 0L
            Row(hPad.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(dateText().day(day), style = Penny.type.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
                if (spent > 0) MoneyText(spent, Penny.type.label, c.textSecondary)
            }
        }
        if (state.rows.isEmpty()) {
            item(key = "none") { Text(stringResource(R.string.calendar_no_entries), style = Penny.type.caption, color = c.textSecondary, modifier = hPad) }
        } else {
            items(state.rows, key = { "tx_" + it.tx.id }) { row ->
                TxRowItem(
                    row, hPad,
                    onClick = { onEditTransaction(row.tx.id) },
                    onDelete = { vm.delete(row.tx) { t -> snack.undo(deletedText) { vm.restore(t) } } },
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthSwitcher(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    val c = Penny.colors
    val dates = dateText()
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.a11y_prev_month), tint = c.textPrimary)
        }
        AnimatedContent(month, modifier = Modifier.weight(1f), label = "month", transitionSpec = { fadeIn() togetherWith fadeOut() }) { m ->
            Text(dates.month(m), style = Penny.type.title, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.a11y_next_month), tint = c.textPrimary)
        }
    }
}

@Composable
private fun summaryText(spent: Long, income: Long): String {
    val parts = listOfNotNull(
        if (spent != 0L) stringResource(R.string.calendar_spent, money(spent)) else null,
        if (income != 0L) stringResource(R.string.calendar_income, money(income)) else null,
    )
    return if (parts.isEmpty()) stringResource(R.string.calendar_month_empty) else parts.joinToString(" · ")
}
