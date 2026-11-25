package com.penny.wallet.feature.calendar

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.components.LocalPrivacy
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.currentLocale
import com.penny.wallet.domain.CalendarDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val CellGap = 2.dp

@Composable
internal fun WeekdayRow(first: DayOfWeek, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    Row(modifier.fillMaxWidth().clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(CellGap)) {
        repeat(7) { i ->
            Text(
                first.plus(i.toLong()).getDisplayName(TextStyle.NARROW_STANDALONE, locale), style = Penny.type.caption,
                color = Penny.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun WeekRow(
    week: List<CalendarDay>, maxDaySpent: Long, today: LocalDate, selected: LocalDate?,
    onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CellGap)) {
        week.forEach { d ->
            DayCell(d, maxDaySpent, d.date == today, d.date == selected, { onSelect(d.date) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, maxDaySpent: Long, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = Penny.colors
    val shape = Penny.dims.smallShape
    val shade = if (day.spent > 0 && maxDaySpent > 0) (0.85f * day.spent / maxDaySpent).coerceIn(0.12f, 0.85f) else 0f
    // Whichever text colour contrasts more with the blended cell, so light theme tints stay readable (WCAG AA).
    val cellColor = c.accent.copy(alpha = shade).compositeOver(c.background)
    val textColor = listOf(c.textPrimary, c.onAccent).maxBy { contrast(it, cellColor) }
    val outline = when {
        isSelected -> Modifier.border(2.dp, c.textPrimary, shape)
        isToday -> Modifier.border(1.dp, c.accent, shape)
        else -> Modifier
    }
    val description = dayDescription(day)
    val todayLabel = stringResource(R.string.date_today)
    Box(
        modifier.heightIn(min = 52.dp).alpha(if (day.inMonth) 1f else 0.35f).clip(shape)
            .background(c.accent.copy(alpha = shade)).then(outline)
            .clickable(role = Role.Button, onClick = onClick)
            // clearAndSetSemantics drops the clickable's action, so TalkBack needs role and click set again here.
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
                this.selected = isSelected
                if (isToday) stateDescription = todayLabel
            }
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.date.dayOfMonth.toString(), style = Penny.type.label, color = textColor, maxLines = 1)
            if (day.spent > 0) CompactAmount(day.spent, textColor)
            else if (day.count > 0) Box(Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(c.accent)) // income or moves only
        }
    }
}

/** Whole units only; when even that does not fit the cell (or amounts are hidden) a dot marks the day. */
@Composable
private fun CompactAmount(cents: Long, color: Color) {
    val p = Penny.prefs
    val hidden = p.hideAmounts && !LocalPrivacy.current.revealed
    val text = Formatters.money(cents, Formatters.of(p, currentLocale()).copy(showCents = false))
    var fits by remember(text) { mutableStateOf(true) }
    if (hidden || !fits) {
        Box(Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(color))
        return
    }
    val base = Penny.type.caption
    Text(
        text, style = base.copy(fontSize = base.fontSize * 0.8f), color = color, maxLines = 1, softWrap = false,
        overflow = TextOverflow.Clip, onTextLayout = { if (it.didOverflowWidth) fits = false },
    )
}

@Composable
private fun dayDescription(day: CalendarDay): String {
    val locale = currentLocale()
    val fmt = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale) }
    val date = fmt.format(day.date)
    return if (day.spent > 0) pluralStringResource(R.plurals.calendar_day_spent, day.count, date, money(day.spent), day.count)
    else pluralStringResource(R.plurals.calendar_day, day.count, date, day.count)
}

private fun contrast(a: Color, b: Color): Float {
    val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (hi + 0.05f) / (lo + 0.05f)
}
