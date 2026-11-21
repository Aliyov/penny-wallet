package com.penny.wallet.feature.savings

import com.penny.wallet.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.data.prefs.GoalLayout
import com.penny.wallet.core.ui.components.AnimatedAmount
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny

@Composable
fun SavingsScreen(onOpenGoal: (String) -> Unit) {
    val vm = pennyViewModel { SavingsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    val c = Penny.colors
    val grid = state.layout == GoalLayout.GRID
    val gap = Penny.dims.gap

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (grid) 2 else 1),
        modifier = Modifier.fillMaxSize().background(c.background).statusBarsPadding(),
        contentPadding = PaddingValues(start = Penny.dims.gutter, end = Penny.dims.gutter, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        item("hero", span = { GridItemSpan(maxLineSpan) }) { SavingsHero(state) }
        items(state.goals, key = { it.goal.id }) { card ->
            if (grid) GoalGridCard(card, Modifier.animateItem()) { onOpenGoal(card.goal.id) }
            else GoalListRow(card, Modifier.animateItem()) { onOpenGoal(card.goal.id) }
        }
        item("new") { NewGoalCard(grid) { creating = true } }
    }

    if (creating) {
        GoalSheet(initial = null, onDismiss = { creating = false }) { name, icon, color, target, deadline, done ->
            vm.create(name, icon, color, target, deadline) { err -> done(err); if (err == null) creating = false }
        }
    }
}

@Composable
private fun SavingsHero(state: SavingsState) {
    val c = Penny.colors
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp)
            .then(if (Penny.motion.enabled) Modifier.animateContentSize() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.savings_total_saved), style = Penny.type.label, color = c.textSecondary)
        AnimatedAmount(state.totalSaved, Penny.type.hero, c.accent)
        val n = state.goals.size
        if (n > 0) Text(
            pluralStringResource(R.plurals.savings_of_target_across, n, money(state.totalTarget), n),
            style = Penny.type.label, color = c.textSecondary,
        ) else if (!state.loading) Text(stringResource(R.string.savings_empty_hint), style = Penny.type.label, color = c.textSecondary)
    }
}
