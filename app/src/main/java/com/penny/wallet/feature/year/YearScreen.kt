package com.penny.wallet.feature.year

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.EmptyState
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.domain.YearOverview
import com.penny.wallet.feature.expenses.CategoryRow
import java.time.YearMonth

@Composable
fun YearScreen(onBack: () -> Unit, onOpenMonth: (YearMonth) -> Unit) {
    val vm = pennyViewModel { YearViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    YearContent(state, onBack, vm::prevYear, vm::nextYear, onOpenMonth)
}

@Composable
internal fun YearContent(state: YearState, onBack: () -> Unit, onOlder: () -> Unit, onNewer: () -> Unit, onOpenMonth: (YearMonth) -> Unit) {
    val c = Penny.colors
    val gutter = Penny.dims.gutter
    val hPad = Modifier.padding(horizontal = gutter)
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = c.textPrimary) }
            Text(stringResource(R.string.year_title), style = Penny.type.title, color = c.textPrimary)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            YearSwitcher(state, onOlder, onNewer)
            val o = state.overview
            if (o != null && !state.loading) {
                if (o.isEmpty()) {
                    EmptyState(stringResource(R.string.year_empty, o.year), hPad)
                } else {
                    TotalsCard(o, hPad)
                    SectionLabel(stringResource(R.string.year_chart), hPad)
                    YearChart(o, onOpenMonth, hPad)
                    if (o.months.any { it.spent != 0L || it.income != 0L }) {
                        SectionLabel(stringResource(R.string.year_by_month), hPad)
                        MonthList(o, onOpenMonth, hPad)
                    }
                    if (state.categories.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.year_top), hPad)
                        state.categories.forEach { s ->
                            CategoryRow(s, active = false, dimmed = false, onClick = null, modifier = Modifier.padding(horizontal = gutter - 8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun YearOverview.isEmpty() = months.all { it.spent == 0L && it.income == 0L && it.saved == 0L }

@Composable
private fun YearSwitcher(state: YearState, onOlder: () -> Unit, onNewer: () -> Unit) {
    val c = Penny.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onOlder, enabled = state.canGoOlder) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.year_prev),
                tint = if (state.canGoOlder) c.textPrimary else c.textSecondary.copy(alpha = 0.3f),
            )
        }
        AnimatedContent(state.year, modifier = Modifier.weight(1f), label = "year", transitionSpec = { fadeIn() togetherWith fadeOut() }) { y ->
            Text(y.toString(), style = Penny.type.title, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        IconButton(onClick = onNewer, enabled = state.canGoNewer) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.year_next),
                tint = if (state.canGoNewer) c.textPrimary else c.textSecondary.copy(alpha = 0.3f),
            )
        }
    }
}
