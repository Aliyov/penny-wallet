package com.penny.wallet.feature.settings

import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.prefs.HeroMetric
import com.penny.wallet.data.prefs.HomeSection
import com.penny.wallet.core.ui.components.ChoiceRow
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.components.StepperRow
import com.penny.wallet.core.ui.components.rememberHaptic
import com.penny.wallet.core.ui.theme.Penny
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val KEY_PREFIX = "homeSection_"

@Composable
private fun HomeSection.label(): String = stringResource(
    when (this) {
        HomeSection.HERO -> R.string.pers_home_hero
        HomeSection.MONTH -> R.string.pers_home_month
        HomeSection.ACCOUNTS -> R.string.pers_home_accounts
        HomeSection.UPCOMING -> R.string.pers_home_upcoming
        HomeSection.RECENT -> R.string.pers_home_recent
    },
)

/** Drag order is kept locally and persisted on drop. */
internal class HomeReorder(val state: ReorderableLazyListState, val order: MutableState<List<HomeSection>>, val persist: () -> Unit)

@Composable
internal fun rememberHomeReorderState(listState: LazyListState, x: PrefsCtx): HomeReorder {
    val saved = (x.p.homeSections + HomeSection.entries).distinct()
    val order = remember { mutableStateOf(saved) }
    val haptic = rememberHaptic()
    val state = rememberReorderableLazyListState(listState) { from, to ->
        val f = (from.key as? String)?.removePrefix(KEY_PREFIX)?.let { k -> HomeSection.entries.find { it.name == k } }
        val t = (to.key as? String)?.removePrefix(KEY_PREFIX)?.let { k -> HomeSection.entries.find { it.name == k } }
        if (f != null && t != null) {
            order.value = order.value.toMutableList().apply { add(indexOf(t), removeAt(indexOf(f))) }
            haptic(HapticFeedbackType.TextHandleMove)
        }
    }
    LaunchedEffect(saved) { if (!state.isAnyItemDragging) order.value = saved }
    val cur by rememberUpdatedState(x)
    return remember(state) { HomeReorder(state, order) { val o = order.value; cur.set { it.copy(homeSections = o) } } }
}

internal fun LazyListScope.homeSection(x: PrefsCtx, r: HomeReorder) {
    val p = x.p
    val d = x.d
    item("homeLabel") {
        val reset = x.reset(p.homeSections to p.hiddenHomeSections, d.homeSections to d.hiddenHomeSections) {
            it.copy(homeSections = d.homeSections, hiddenHomeSections = d.hiddenHomeSections)
        }
        Column {
            SectionLabel(stringResource(R.string.pers_section_home), Modifier.padding(top = 8.dp), action = if (reset != null) stringResource(R.string.pers_reset_layout) else null, onAction = reset)
            Text(stringResource(R.string.pers_home_hint), style = Penny.type.caption, color = Penny.colors.textSecondary)
        }
    }
    items(r.order.value, key = { KEY_PREFIX + it.name }) { s ->
        ReorderableItem(r.state, KEY_PREFIX + s.name) { dragging ->
            val c = Penny.colors
            val visible = s !in p.hiddenHomeSections
            val lastVisible = visible && HomeSection.entries.count { it !in p.hiddenHomeSections } <= 1
            Row(
                Modifier.fillMaxWidth().clip(Penny.dims.smallShape).background(if (dragging) c.surface2 else c.surface1)
                    .border(0.75.dp, if (dragging) c.accent.copy(alpha = 0.6f) else c.hairline, Penny.dims.smallShape)
                    .padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {}, modifier = Modifier.draggableHandle(onDragStopped = r.persist)) {
                    Icon(Icons.Rounded.DragHandle, stringResource(R.string.pers_drag_section, s.label()), tint = c.textSecondary)
                }
                Text(s.label(), style = Penny.type.bodyStrong, color = if (visible) c.textPrimary else c.textSecondary, modifier = Modifier.weight(1f))
                Switch(
                    visible, enabled = !lastVisible,
                    onCheckedChange = { show ->
                        x.set { it.copy(hiddenHomeSections = if (show) it.hiddenHomeSections - s else it.hiddenHomeSections + s) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                        uncheckedThumbColor = c.textSecondary, uncheckedTrackColor = c.surface1, uncheckedBorderColor = c.hairline,
                    ),
                )
            }
        }
    }
    item("homeOptions") {
        PennyCard(Modifier.fillMaxWidth()) {
            ChoiceRow(
                stringResource(R.string.pers_hero_shows),
                listOf(
                    HeroMetric.AVAILABLE to stringResource(R.string.pers_hero_available), HeroMetric.TOTAL to stringResource(R.string.pers_hero_total),
                    HeroMetric.SPENT_MONTH to stringResource(R.string.pers_hero_spent_month),
                ),
                p.heroMetric, { v -> x.set { it.copy(heroMetric = v) } },
                onReset = x.reset(p.heroMetric, d.heroMetric) { it.copy(heroMetric = d.heroMetric) },
            )
            StepperRow(
                stringResource(R.string.pers_recent_count), p.recentCount, 3..10, { v -> x.set { it.copy(recentCount = v) } },
                onReset = x.reset(p.recentCount, d.recentCount) { it.copy(recentCount = d.recentCount) },
            )
        }
    }
}
