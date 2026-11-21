package com.penny.wallet.feature.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.core.ui.components.PennyChip
import com.penny.wallet.core.ui.theme.Penny
import java.time.format.TextStyle

/** Frequency-specific anchor picker: day of month / weekday / month + day. */
@Composable
internal fun SchedulePicker(d: RuleDraft, onChange: (RuleDraft) -> Unit) {
    val c = Penny.colors
    when (d.frequency) {
        Frequency.WEEKLY -> {
            FieldLabel("On")
            ChipStrip((1..7).toList(), d.dayOfWeek, { weekdayName(it, TextStyle.SHORT) }) { onChange(d.copy(dayOfWeek = it)) }
        }
        Frequency.MONTHLY -> {
            FieldLabel("Day of month")
            DayStrip(d.dayOfMonth) { onChange(d.copy(dayOfMonth = it)) }
            Text("Short months use the last day", style = Penny.type.caption, color = c.textSecondary)
        }
        Frequency.YEARLY -> {
            FieldLabel("Month")
            ChipStrip((1..12).toList(), d.month, { monthName(it) }) { onChange(d.copy(month = it)) }
            FieldLabel("Day")
            DayStrip(d.dayOfMonth) { onChange(d.copy(dayOfMonth = it)) }
            if (d.month == 2 && d.dayOfMonth > 28) {
                Text("In non-leap years the last day of February is used", style = Penny.type.caption, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun DayStrip(selected: Int, onSelect: (Int) -> Unit) =
    ChipStrip((1..31).toList(), selected, { it.toString() }, onSelect)

@Composable
private fun ChipStrip(values: List<Int>, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit) {
    val scroll = rememberScrollState()
    Row(Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { v -> PennyChip(label(v), v == selected, { onSelect(v) }, Modifier.widthIn(min = 44.dp)) }
    }
}

/** 0–7 days before; 0 = "Same day". */
@Composable
internal fun RemindStepper(days: Int, onChange: (Int) -> Unit) {
    val c = Penny.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            when (days) { 0 -> "Same day"; 1 -> "1 day before"; else -> "$days days before" },
            style = Penny.type.body, color = c.textPrimary, modifier = Modifier.weight(1f),
        )
        StepButton(Icons.Rounded.Remove, "Fewer days", days > 0) { onChange(days - 1) }
        Text(days.toString(), style = Penny.type.bodyStrong, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 36.dp))
        StepButton(Icons.Rounded.Add, "More days", days < 7) { onChange(days + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Penny.colors
    Box(
        Modifier.size(40.dp).alpha(if (enabled) 1f else 0.35f).clip(CircleShape).background(c.surface1)
            .border(0.75.dp, c.hairline, CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = c.accent, modifier = Modifier.size(20.dp)) }
}

@Composable
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Penny.colors
    Row(Modifier.fillMaxWidth().clip(Penny.dims.smallShape).clickable { onChange(!checked) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = Penny.type.bodyStrong, color = c.textPrimary)
            Text(subtitle, style = Penny.type.caption, color = c.textSecondary)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                uncheckedThumbColor = c.textSecondary, uncheckedTrackColor = c.surface1, uncheckedBorderColor = c.hairline,
            ),
        )
    }
}

/** Survives rotation / process death while the sheet is open. */
internal val RuleDraftSaver: Saver<RuleDraft, Any> = listSaver(
    save = { listOf(it.id, it.name, it.amountCents, it.categoryId, it.accountId, it.frequency.name, it.dayOfMonth, it.month, it.dayOfWeek, it.remindDaysBefore, it.autoPay, it.active) },
    restore = {
        RuleDraft(
            it[0] as String?, it[1] as String, it[2] as Long, it[3] as String?, it[4] as String?, Frequency.valueOf(it[5] as String),
            it[6] as Int, it[7] as Int, it[8] as Int, it[9] as Int, it[10] as Boolean, it[11] as Boolean,
        )
    },
)
