package com.penny.wallet.ui.quickadd

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.prefs.QuickType
import com.penny.wallet.ui.components.Numpad
import com.penny.wallet.ui.components.PennyButton
import com.penny.wallet.ui.components.PennySheet
import com.penny.wallet.ui.components.SegmentedControl
import com.penny.wallet.ui.components.money
import com.penny.wallet.ui.components.rememberHaptic
import com.penny.wallet.ui.theme.Penny

private val TYPES = listOf(QuickType.EXPENSE, QuickType.INCOME, QuickType.SAVE, QuickType.TRANSFER)
private val LABELS = listOf("Expense", "Income", "Save", "Transfer")

/** Quick add (+) sheet, SPEC §8: type switch, huge amount, context chips, optional row, numpad. */
@Composable
fun QuickAddSheet(
    vm: QuickAddViewModel,
    receipt: ReceiptLaunchers,
    onDismiss: () -> Unit,
    onResult: (QuickResult.Saved) -> Unit,
    onDeleted: (Transaction) -> Unit,
) {
    val form by vm.form.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    val c = Penny.colors
    val haptic = rememberHaptic()
    var error by remember { mutableStateOf<String?>(null) }
    var errorTick by remember { mutableIntStateOf(0) }

    val handle: (QuickResult) -> Unit = { r ->
        when (r) {
            is QuickResult.Saved -> { haptic(HapticFeedbackType.Confirm); error = null; onResult(r) }
            is QuickResult.Error -> { haptic(HapticFeedbackType.Reject); error = r.message; errorTick++ }
        }
    }
    // Any edit clears a stale error.
    LaunchedEffect(form.digits, form.type, form.accountId, form.toAccountId, form.goalId) { error = null }

    val editing = form.editing
    // Withdrawal / adjustment / unsave edits: amount, date, note only.
    val simpleEdit = editing != null && editing.type !in setOf(TxType.EXPENSE, TxType.INCOME, TxType.TRANSFER, TxType.SAVE)

    ReceiptScanDialog(vm, receipt)

    PennySheet(onDismiss = onDismiss) {
        if (editing == null) {
            SegmentedControl(LABELS, TYPES.indexOf(form.type).coerceAtLeast(0), { i -> vm.edit { it.copy(type = TYPES[i]) } })
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Edit", style = Penny.type.title, color = c.textPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.deleteEditing { t -> if (t != null) onDeleted(t) } }) {
                    Text("Delete", style = Penny.type.bodyStrong, color = c.danger)
                }
            }
        }

        val cents = form.cents
        Text(
            money(cents, ignorePrivacy = true),
            style = Penny.type.hero,
            color = when {
                cents == 0L -> c.textSecondary
                form.type == QuickType.EXPENSE && !simpleEdit -> c.textPrimary
                else -> c.accent
            },
            maxLines = 1, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                .then(if (Penny.motion.enabled) Modifier.animateContentSize(Penny.motion.spec()) else Modifier),
        )
        ErrorLine(error, errorTick)

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!simpleEdit) ContextRow(form, data, vm, handle)
            OptionalRow(form, data, vm::edit) {
                if (form.type == QuickType.EXPENSE && editing == null) ReceiptChip(receipt)
            }
        }
        Spacer(Modifier.height(10.dp))
        Numpad(form.digits, { d -> vm.edit { it.copy(digits = d) } }, keyHeight = 56)
        // Always visible: with save-on-chip-tap a category tap is just a shortcut. Hiding the button left the
        // user stuck when they picked the category before typing the amount.
        Spacer(Modifier.height(10.dp))
        PennyButton(if (editing != null) "Save changes" else "Save", { vm.save(handle) }, Modifier.fillMaxWidth(), enabled = cents > 0)
    }
}

@Composable
private fun ContextRow(form: QuickForm, data: QuickData, vm: QuickAddViewModel, handle: (QuickResult) -> Unit) {
    val c = Penny.colors
    when (form.type) {
        QuickType.EXPENSE -> CategoryChips(data.categories, form.categoryId) { id -> vm.onCategory(id, handle) }
        QuickType.INCOME -> Unit
        QuickType.SAVE -> {
            GoalChips(data.goals, form.goalId) { id -> vm.edit { it.copy(goalId = id) } }
            val acc = data.accounts.firstOrNull { it.id == form.accountId }
            if (acc != null) {
                Text(
                    "Available in ${acc.name}: ${money(data.available[acc.id] ?: 0L)}",
                    style = Penny.type.caption, color = c.textSecondary,
                )
            }
        }
        QuickType.TRANSFER -> AccountChips(data.accounts.filter { it.id != form.accountId }, form.toAccountId,
            { id -> vm.edit { it.copy(toAccountId = id) } }, "To")
    }
}

/** Inline error in danger colour, with a short horizontal shake when motion is enabled. */
@Composable
private fun ErrorLine(error: String?, tick: Int) {
    val shake = remember { Animatable(0f) }
    val motion = Penny.motion.enabled
    LaunchedEffect(tick) {
        if (tick > 0 && motion) {
            shake.snapTo(0f)
            shake.animateTo(0f, keyframes {
                durationMillis = 360
                -10f at 50; 10f at 110; -8f at 170; 6f at 230; -3f at 290
            })
        }
    }
    Text(
        error ?: "",
        style = Penny.type.caption, color = Penny.colors.danger, textAlign = TextAlign.Center, maxLines = 1,
        modifier = Modifier.fillMaxWidth().offset(x = shake.value.dp).padding(bottom = 6.dp),
    )
}
