package com.penny.wallet.feature.recurring

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny

/** Recurring expenses list (SPEC §5): add / edit / pause / delete, next due, "Pay now". */
@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val vm = pennyViewModel { RecurringViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val c = Penny.colors
    var editing by remember { mutableStateOf<RuleDraft?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.textPrimary) }
            Text("Recurring", style = Penny.type.title, color = c.textPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = { editing = RuleDraft(remindDaysBefore = state.defaultRemindDays) }) {
                Icon(Icons.Rounded.Add, "Add recurring expense", tint = c.accent)
            }
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = Penny.dims.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            if (state.rows.isEmpty()) {
                item("empty") { EmptyState { editing = RuleDraft(remindDaysBefore = state.defaultRemindDays) } }
            } else {
                item("header") { HeaderCard(state) }
                item("label") { SectionLabel("Rules", Modifier.padding(top = 8.dp)) }
                items(state.rows, key = { it.rule.id }) { row ->
                    RuleCard(
                        row, Modifier.animateItem(),
                        onEdit = { editing = row.rule.toDraft() },
                        onPayNow = {
                            vm.payNow(row.rule) { p ->
                                if (p != null) snack.undo("Paid ${row.rule.name}") { vm.undoPay(p) } else snack.show("Already paid")
                            }
                        },
                        onToggleActive = { vm.setActive(row.rule, !row.rule.active) },
                        onDelete = { vm.delete(row.rule) { r -> snack.undo("Deleted") { vm.restore(r) } } },
                    )
                }
            }
            item("bottom") { Spacer(Modifier.height(32.dp)) }
        }
    }

    editing?.let { d ->
        RuleSheet(d, state, onDismiss = { editing = null }) { draft, showError ->
            vm.save(draft) { err, ask ->
                if (err != null) showError(err)
                else {
                    editing = null
                    if (ask && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(state: RecurringState) {
    val c = Penny.colors
    val active = state.rows.count { it.rule.active }
    PennyCard(Modifier.fillMaxWidth()) {
        Text("Per month", style = Penny.type.label, color = c.textSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("≈ ", style = Penny.type.amountLarge, color = c.textSecondary)
            MoneyText(state.monthlyTotal, Penny.type.amountLarge, c.textPrimary)
            Text(" / month", style = Penny.type.body, color = c.textSecondary, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(
            if (active == 1) "1 active rule" else "$active active rules",
            style = Penny.type.caption, color = c.textSecondary, modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val c = Penny.colors
    Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No recurring expenses", style = Penny.type.title, color = c.textPrimary)
        Text(
            "Add subscriptions, rent or phone credit. Penny reminds you before they're due and can record them automatically.",
            style = Penny.type.body, color = c.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        PennyButton("Add recurring expense", onAdd)
    }
}
