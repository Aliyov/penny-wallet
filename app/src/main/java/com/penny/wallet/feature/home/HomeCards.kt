package com.penny.wallet.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.components.AnimatedAmount
import com.penny.wallet.core.ui.components.IconBadge
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.theme.Penny

@Composable
fun MonthCard(state: HomeState, modifier: Modifier = Modifier) {
    val c = Penny.colors
    PennyCard(modifier.fillMaxWidth().animateContentSize(sizeSpec())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_spent_this_month), style = Penny.type.caption, color = c.textSecondary)
                Spacer(Modifier.height(4.dp))
                AnimatedAmount(state.spentThisMonth, Penny.type.amountLarge, c.textPrimary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${state.daysLeft}", style = Penny.type.amountLarge, color = c.textPrimary)
                Text(pluralStringResource(R.plurals.home_days_left, state.daysLeft), style = Penny.type.caption, color = c.textSecondary)
            }
        }
        if (state.dueThisMonth > 0) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyText(state.dueThisMonth, Penny.type.label, c.warning)
                Text(stringResource(R.string.home_due_this_month), style = Penny.type.label, color = c.textSecondary)
            }
        }
    }
}

@Composable
fun AccountCarousel(accounts: List<AccountCard>, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val gutter = Penny.dims.gutter
    Column {
        SectionLabel(stringResource(R.string.home_accounts), Modifier.padding(horizontal = gutter))
        LazyRow(
            contentPadding = PaddingValues(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            items(accounts, key = { it.account.id }) { a -> AccountTile(a, { onOpen(a.account.id) }, Modifier.animateItem()) }
            item(key = "add") { AddAccountTile(onAdd) }
        }
    }
}

private val TileWidth = 200.dp
private val TileHeight = 150.dp

@Composable
private fun AccountTile(a: AccountCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val avail = a.money.available
    PennyCard(modifier.width(TileWidth).height(TileHeight).animateContentSize(sizeSpec()), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(a.account.icon, c.accent, size = 34)
            Spacer(Modifier.width(10.dp))
            Text(a.account.name, style = Penny.type.bodyStrong, color = c.textPrimary, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        AnimatedAmount(a.money.balance, Penny.type.amount, c.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_tile_available), style = Penny.type.caption, color = c.textSecondary)
            MoneyText(avail, Penny.type.caption, if (avail < 0) c.danger else c.accent)
        }
    }
}

@Composable
private fun AddAccountTile(onClick: () -> Unit) {
    val c = Penny.colors
    val shape = Penny.dims.cardShape
    Box(
        Modifier.width(140.dp).height(TileHeight).clip(shape).border(0.75.dp, c.hairline, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Add, null, tint = c.accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.home_add_account), style = Penny.type.label, color = c.accent)
        }
    }
}
