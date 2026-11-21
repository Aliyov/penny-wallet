package com.penny.wallet.feature.savings

import com.penny.wallet.R
import androidx.compose.ui.res.stringResource
import com.penny.wallet.core.util.currentLocale
import com.penny.wallet.core.util.dateText
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.core.ui.components.AmountSheet
import com.penny.wallet.core.ui.components.AnimatedAmount
import com.penny.wallet.core.ui.components.ButtonKind
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyChip
import com.penny.wallet.core.ui.components.ProgressRing
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny

@Composable
internal fun GoalTopBar(name: String, onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val c = Penny.colors
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = c.textPrimary) }
        Text(name, style = Penny.type.title, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.savings_more), tint = c.textPrimary) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surface2) {
                DropdownMenuItem(text = { Text(stringResource(R.string.action_edit), color = c.textPrimary) }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = c.danger) }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

@Composable
internal fun GoalHero(card: GoalCard) {
    val c = Penny.colors
    val color = Color(card.goal.color)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 16.dp).then(if (Penny.motion.enabled) Modifier.animateContentSize() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressRing(card.ring, color, Modifier.size(220.dp), stroke = 18.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Formatters.percent(card.progress, currentLocale()), style = Penny.type.title, color = color)
                AnimatedAmount(card.balance, Penny.type.amountLarge, c.textPrimary)
                Text(stringResource(R.string.savings_of_target, money(card.goal.targetCents)), style = Penny.type.label, color = c.textSecondary)
            }
        }
        if (card.reached) {
            Text(stringResource(R.string.savings_reached), style = Penny.type.bodyStrong, color = c.accent, modifier = Modifier.padding(top = 16.dp))
        } else {
            Text(stringResource(R.string.savings_to_go, money(card.remaining)), style = Penny.type.bodyStrong, color = c.textPrimary, modifier = Modifier.padding(top = 16.dp))
        }
        card.monthlyNeeded?.takeIf { it > 0 }?.let { m ->
            Text(stringResource(R.string.savings_monthly_for_deadline, money(m)), style = Penny.type.label, color = c.textSecondary)
        }
        card.goal.deadline?.let { d -> Text(stringResource(R.string.savings_deadline_on, dateText().fullDate(d)), style = Penny.type.label, color = c.textSecondary) }
    }
}

@Composable
internal fun GoalActions(onAdd: () -> Unit, onTakeBack: () -> Unit, canTakeBack: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(Penny.dims.gap)) {
        PennyButton(stringResource(R.string.savings_add_money), onAdd, Modifier.weight(1f))
        PennyButton(stringResource(R.string.savings_take_back), onTakeBack, Modifier.weight(1f), kind = ButtonKind.SECONDARY, enabled = canTakeBack)
    }
}

/** Amount sheet with account chips. Shows each account's available (add) or its money in this goal (take back). */
@Composable
internal fun GoalMoneySheet(
    title: String, confirm: String, accounts: List<AccountChoice>, outside: Long, initialAccount: String?, showAvailable: Boolean,
    onDismiss: () -> Unit, onConfirm: (cents: Long, accountId: String) -> Unit,
) {
    var accountId by rememberSaveable { mutableStateOf(initialAccount) }
    val c = Penny.colors
    AmountSheet(
        title = title, onDismiss = onDismiss, confirmLabel = confirm,
        extra = {
            Text(stringResource(if (showAvailable) R.string.savings_from else R.string.savings_back_to), style = Penny.type.label, color = c.textSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { a ->
                    val amount = if (showAvailable) a.available else a.inGoal
                    PennyChip(stringResource(R.string.savings_account_amount, a.account.name, money(amount)), accountId == a.account.id, { accountId = a.account.id })
                }
                // Money from outside the tracked accounts: doesn't touch any balance.
                val ext = MoneyCalculator.EXTERNAL
                PennyChip(if (showAvailable) stringResource(R.string.savings_outside_add) else stringResource(R.string.savings_outside_amount, money(outside)), accountId == ext, { accountId = ext })
            }
        },
    ) { cents ->
        val id = accountId ?: return@AmountSheet
        onConfirm(cents, id)
    }
}
