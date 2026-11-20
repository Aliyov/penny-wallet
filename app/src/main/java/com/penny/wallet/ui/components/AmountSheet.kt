package com.penny.wallet.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.penny.wallet.domain.Formatters
import com.penny.wallet.ui.theme.Penny
import kotlin.math.abs

/** Huge amount readout used by amount sheets and quick add. Accent when positive, danger when negative. */
@Composable
fun AmountDisplay(cents: Long, negative: Boolean = false, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val shown = if (negative) -abs(cents) else abs(cents)
    Text(
        money(shown, ignorePrivacy = true),
        style = Penny.type.hero,
        color = when { negative && cents != 0L -> c.danger; cents == 0L -> c.textSecondary; else -> c.accent },
        maxLines = 1, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp).then(if (Penny.motion.enabled) Modifier.animateContentSize() else Modifier),
    )
}

/** Small selectable pill chip used in sheets. */
@Composable
fun PennyChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier.clip(shape).background(if (selected) c.accent.copy(alpha = 0.18f) else c.surface1)
            .border(0.75.dp, if (selected) c.accent else c.hairline, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Penny.type.label, color = if (selected) c.accent else c.textPrimary) }
}

/**
 * Generic "enter an amount" sheet: hero amount, optional ± toggle, [extra] slot, numpad, confirm.
 * Returns cents (negative only when [allowNegative] and the toggle is on).
 */
@Composable
fun AmountSheet(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    initialCents: Long = 0,
    allowZero: Boolean = false,
    allowNegative: Boolean = false,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
    onConfirm: (cents: Long) -> Unit,
) {
    var digits by rememberSaveable { mutableStateOf(NumpadLogic.fromCents(abs(initialCents))) }
    var negative by rememberSaveable { mutableStateOf(allowNegative && initialCents < 0) }
    val cents = Formatters.digitsToCents(digits)
    PennySheet(onDismiss = onDismiss, title = title) {
        AmountDisplay(cents, negative)
        if (allowNegative) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                PennyChip(if (negative) "− Negative" else "± Positive", negative, { negative = !negative })
            }
            Spacer(Modifier.height(8.dp))
        }
        if (extra != null) {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(Penny.dims.gap)) { extra() }
        }
        Spacer(Modifier.height(8.dp))
        Numpad(digits, { digits = it })
        Spacer(Modifier.height(Penny.dims.gap))
        PennyButton(
            confirmLabel,
            onClick = { onConfirm(if (negative) -cents else cents) },
            modifier = Modifier.fillMaxWidth(),
            enabled = cents != 0L || allowZero,
        )
    }
}
