package com.penny.wallet.ui.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.domain.Formatters
import com.penny.wallet.ui.account.pennyFieldColors
import com.penny.wallet.ui.components.AmountDisplay
import com.penny.wallet.ui.components.DatePickerRow
import com.penny.wallet.ui.components.Numpad
import com.penny.wallet.ui.components.NumpadLogic
import com.penny.wallet.ui.components.PennyButton
import com.penny.wallet.ui.components.PennyChip
import com.penny.wallet.ui.components.PennyIcons
import com.penny.wallet.ui.components.PennySheet
import com.penny.wallet.ui.theme.ItemPalette
import com.penny.wallet.ui.theme.Penny
import java.time.LocalDate

/** Callback for saving a goal: values + `done(error)` that the sheet uses to show validation errors. */
typealias GoalSave = (name: String, icon: String, color: Long, targetCents: Long, deadline: LocalDate?, done: (String?) -> Unit) -> Unit

/** New / edit goal sheet (SPEC §8 Savings): name, icon, colour, required target, optional deadline. */
@Composable
internal fun GoalSheet(initial: Goal?, onDismiss: () -> Unit, onSave: GoalSave) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: "savings") }
    var color by rememberSaveable { mutableStateOf(initial?.color ?: ItemPalette[1]) }
    var digits by rememberSaveable { mutableStateOf(NumpadLogic.fromCents(initial?.targetCents ?: 0)) }
    var deadline by rememberSaveable { mutableStateOf(initial?.deadline) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by rememberSaveable { mutableStateOf(false) }
    val target = Formatters.digitsToCents(digits)
    val c = Penny.colors
    val tint = Color(color)

    PennySheet(onDismiss = onDismiss, title = if (initial == null) "New goal" else "Edit goal") {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                name, { name = it; error = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Name") },
                textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
            )
            Label("Icon")
            IconGrid(icon, tint) { icon = it }
            Label("Colour")
            ColourRow(color) { color = it }
            Label("Target")
            AmountDisplay(target)
            Numpad(digits, { digits = it; error = null }, keyHeight = 52)
            Label("Deadline (optional)")
            val d = deadline
            if (d == null) {
                PennyChip("+ Add deadline", false, { deadline = LocalDate.now().plusMonths(6) })
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatePickerRow(d, { deadline = it }, Modifier.weight(1f))
                    PennyChip("Clear", false, { deadline = null })
                }
            }
            error?.let { Text(it, style = Penny.type.label, color = c.danger, modifier = Modifier.padding(top = 12.dp)) }
            PennyButton(
                if (initial == null) "Create goal" else "Save",
                onClick = {
                    busy = true
                    onSave(name.trim(), icon, color, target, deadline) { err -> busy = false; error = err }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = !busy && name.isNotBlank() && target > 0,
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = Penny.type.label, color = Penny.colors.textSecondary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
private fun IconGrid(selected: String, tint: Color, onPick: (String) -> Unit) {
    val c = Penny.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PennyIcons.all.forEach { (k, v) ->
            val sel = k == selected
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(if (sel) tint.copy(alpha = 0.18f) else c.surface1)
                    .border(if (sel) 1.5.dp else 0.75.dp, if (sel) tint else c.hairline, CircleShape)
                    .clickable { onPick(k) },
                contentAlignment = Alignment.Center,
            ) { Icon(v, k, tint = if (sel) tint else c.textPrimary, modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun ColourRow(selected: Long, onPick: (Long) -> Unit) {
    val c = Penny.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ItemPalette.forEach { argb ->
            val sel = argb == selected
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .border(if (sel) 2.dp else 0.dp, if (sel) c.textPrimary else Color.Transparent, CircleShape)
                    .padding(4.dp).clip(CircleShape).background(Color(argb)).clickable { onPick(argb) },
            )
        }
    }
}
