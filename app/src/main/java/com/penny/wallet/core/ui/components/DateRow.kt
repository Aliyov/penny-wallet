package com.penny.wallet.core.ui.components

import com.penny.wallet.R
import androidx.compose.ui.res.stringResource
import com.penny.wallet.core.util.dateText
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.theme.Penny
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Row showing a date ("Today", "Mon 3 Nov"); tap opens a themed Material date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerRow(date: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    var open by remember { mutableStateOf(false) }
    val shape = Penny.dims.smallShape
    Row(
        modifier.fillMaxWidth().clip(shape).border(0.75.dp, c.hairline, shape).clickable { open = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
        Text(dateText().day(date), style = Penny.type.body, color = c.textPrimary)
    }
    if (!open) return
    val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    val colors = DatePickerDefaults.colors(
        containerColor = c.surface2, titleContentColor = c.textSecondary, headlineContentColor = c.textPrimary,
        weekdayContentColor = c.textSecondary, subheadContentColor = c.textSecondary,
        yearContentColor = c.textPrimary, currentYearContentColor = c.accent,
        selectedYearContainerColor = c.accent, selectedYearContentColor = c.onAccent,
        dayContentColor = c.textPrimary, selectedDayContainerColor = c.accent, selectedDayContentColor = c.onAccent,
        todayContentColor = c.accent, todayDateBorderColor = c.accent, dividerColor = c.hairline,
        navigationContentColor = c.textPrimary,
    )
    DatePickerDialog(
        onDismissRequest = { open = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                open = false
            }) { Text(stringResource(R.string.action_ok), color = c.accent, style = Penny.type.bodyStrong) }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel), color = c.textSecondary) } },
        colors = colors,
    ) { DatePicker(state = state, colors = colors) }
}
