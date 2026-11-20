package com.penny.wallet.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.penny.wallet.domain.Formatters
import com.penny.wallet.ui.components.MoneyText
import com.penny.wallet.ui.components.Numpad
import com.penny.wallet.ui.components.PennyButton
import com.penny.wallet.ui.components.PennyIcons
import com.penny.wallet.ui.components.PennySheet
import com.penny.wallet.ui.components.clickableNoRipple
import com.penny.wallet.ui.theme.Penny

/** New account: name, icon, opening balance (numpad, cash-register entry). */
@Composable
fun AddAccountSheet(onDismiss: () -> Unit, onSave: (name: String, icon: String, openingBalanceCents: Long) -> Unit) {
    val c = Penny.colors
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(PennyIcons.accountKeys.first()) }
    var digits by rememberSaveable { mutableStateOf("") }

    PennySheet(onDismiss = onDismiss, title = "New account") {
        NameField(name, { name = it.take(40) })
        Spacer(Modifier.height(14.dp))
        IconGrid(icon) { icon = it }
        Spacer(Modifier.height(14.dp))
        Text("Opening balance", style = Penny.type.caption, color = c.textSecondary)
        MoneyText(
            Formatters.digitsToCents(digits), Penny.type.amountLarge,
            if (digits.isEmpty()) c.textSecondary else c.textPrimary,
            Modifier.padding(vertical = 6.dp),
        )
        Numpad(digits, { digits = it }, keyHeight = 52)
        Spacer(Modifier.height(14.dp))
        PennyButton(
            "Save", { onSave(name.trim(), icon, Formatters.digitsToCents(digits)) },
            Modifier.fillMaxWidth(), enabled = name.isNotBlank(),
        )
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit) {
    val c = Penny.colors
    val shape = Penny.dims.smallShape
    BasicTextField(
        value = value, onValueChange = onChange, singleLine = true,
        textStyle = Penny.type.body.copy(color = c.textPrimary),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(shape).background(c.surface1).border(0.75.dp, c.hairline, shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty()) Text("Account name", style = Penny.type.body, color = c.textSecondary)
                inner()
            }
        },
    )
}

@Composable
private fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    val perRow = 8
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PennyIcons.accountKeys.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key -> IconCell(key, key == selected, Modifier.weight(1f)) { onSelect(key) } }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun IconCell(key: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Penny.colors
    val bg by animateColorAsState(if (selected) c.accent.copy(alpha = 0.2f) else c.surface1, Penny.motion.spec(), label = "icon")
    val fg by animateColorAsState(if (selected) c.accent else c.textSecondary, Penny.motion.spec(), label = "iconFg")
    Box(
        modifier.aspectRatio(1f).clip(CircleShape).background(bg)
            .border(0.75.dp, if (selected) c.accent else c.hairline, CircleShape).clickableNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(PennyIcons.get(key), null, tint = fg, modifier = Modifier.size(20.dp)) }
}
