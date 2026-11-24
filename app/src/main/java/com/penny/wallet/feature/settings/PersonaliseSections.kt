package com.penny.wallet.feature.settings

import java.time.DayOfWeek
import com.penny.wallet.core.util.dateText
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import androidx.compose.foundation.lazy.LazyListScope
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.prefs.ChartStyle
import com.penny.wallet.data.prefs.ChipOrder
import com.penny.wallet.data.prefs.GoalLayout
import com.penny.wallet.data.prefs.GoalSort
import com.penny.wallet.data.prefs.QuickType
import com.penny.wallet.core.ui.components.ChoiceRow
import com.penny.wallet.core.ui.components.SwitchRow

internal fun LazyListScope.quickAddSection(x: PrefsCtx, accounts: List<Account>) {
    val p = x.p
    val d = x.d
    settingsCard("quickAdd", R.string.pers_section_quick_add) {
        ChoiceRow(
            stringResource(R.string.pers_default_type),
            listOf(QuickType.EXPENSE to stringResource(R.string.type_expense), QuickType.INCOME to stringResource(R.string.type_income)), p.quickAddDefaultType,
            { v -> x.set { it.copy(quickAddDefaultType = v) } },
            onReset = x.reset(p.quickAddDefaultType, d.quickAddDefaultType) { it.copy(quickAddDefaultType = d.quickAddDefaultType) },
        )
        // An archived or deleted fixed account falls back to "last used".
        val fixed = p.fixedAccountId?.takeIf { id -> accounts.any { it.id == id } }
        ChoiceRow(
            stringResource(R.string.pers_default_account),
            listOf<Pair<String?, String>>(null to stringResource(R.string.pers_last_used)) + accounts.map { it.id to it.name }, fixed,
            { v -> x.set { it.copy(fixedAccountId = v) } },
            onReset = x.reset(p.fixedAccountId, d.fixedAccountId) { it.copy(fixedAccountId = d.fixedAccountId) },
        )
        ChoiceRow(
            stringResource(R.string.pers_chip_order),
            listOf(
                ChipOrder.MOST_USED to stringResource(R.string.pers_chip_most_used), ChipOrder.MANUAL to stringResource(R.string.pers_manual),
                ChipOrder.ALPHABETICAL to stringResource(R.string.pers_chip_az),
            ),
            p.chipOrder,
            { v -> x.set { it.copy(chipOrder = v) } }, onReset = x.reset(p.chipOrder, d.chipOrder) { it.copy(chipOrder = d.chipOrder) },
        )
        SwitchRow(
            stringResource(R.string.pers_save_on_tap), p.saveOnChipTap, { v -> x.set { it.copy(saveOnChipTap = v) } },
            subtitle = stringResource(R.string.pers_save_on_tap_hint),
            onReset = x.reset(p.saveOnChipTap, d.saveOnChipTap) { it.copy(saveOnChipTap = d.saveOnChipTap) },
        )
        SwitchRow(
            stringResource(R.string.pers_show_note), p.showNoteField, { v -> x.set { it.copy(showNoteField = v) } },
            onReset = x.reset(p.showNoteField, d.showNoteField) { it.copy(showNoteField = d.showNoteField) },
        )
    }
}

internal fun LazyListScope.chartsSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("charts", R.string.pers_section_charts) {
        ChoiceRow(
            stringResource(R.string.pers_default_chart),
            listOf(ChartStyle.DONUT to stringResource(R.string.pers_chart_donut), ChartStyle.BARS to stringResource(R.string.pers_chart_bars)), p.defaultChart,
            { v -> x.set { it.copy(defaultChart = v) } }, onReset = x.reset(p.defaultChart, d.defaultChart) { it.copy(defaultChart = d.defaultChart) },
        )
        ChoiceRow(
            stringResource(R.string.pers_history_months), listOf(3 to "3", 6 to "6", 12 to "12"), p.historyMonths,
            { v -> x.set { it.copy(historyMonths = v) } }, onReset = x.reset(p.historyMonths, d.historyMonths) { it.copy(historyMonths = d.historyMonths) },
        )
        ChoiceRow(
            stringResource(R.string.pers_first_day_week),
            listOf(1 to dateText().weekday(DayOfWeek.MONDAY), 7 to dateText().weekday(DayOfWeek.SUNDAY)), p.firstDayOfWeek,
            { v -> x.set { it.copy(firstDayOfWeek = v) } },
            onReset = x.reset(p.firstDayOfWeek, d.firstDayOfWeek) { it.copy(firstDayOfWeek = d.firstDayOfWeek) },
        )
    }
}

internal fun LazyListScope.savingsSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("savings", R.string.pers_section_savings) {
        ChoiceRow(
            stringResource(R.string.pers_goal_layout),
            listOf(GoalLayout.GRID to stringResource(R.string.pers_layout_grid), GoalLayout.LIST to stringResource(R.string.pers_layout_list)), p.goalLayout,
            { v -> x.set { it.copy(goalLayout = v) } }, onReset = x.reset(p.goalLayout, d.goalLayout) { it.copy(goalLayout = d.goalLayout) },
        )
        ChoiceRow(
            stringResource(R.string.pers_goal_sort),
            listOf(
                GoalSort.PROGRESS to stringResource(R.string.pers_sort_progress), GoalSort.DEADLINE to stringResource(R.string.pers_sort_deadline),
                GoalSort.MANUAL to stringResource(R.string.pers_manual),
            ),
            p.goalSort,
            { v -> x.set { it.copy(goalSort = v) } }, onReset = x.reset(p.goalSort, d.goalSort) { it.copy(goalSort = d.goalSort) },
        )
    }
}
