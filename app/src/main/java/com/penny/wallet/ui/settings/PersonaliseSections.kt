package com.penny.wallet.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.prefs.ChartStyle
import com.penny.wallet.data.prefs.ChipOrder
import com.penny.wallet.data.prefs.GoalLayout
import com.penny.wallet.data.prefs.GoalSort
import com.penny.wallet.data.prefs.QuickType
import com.penny.wallet.ui.components.ChoiceRow
import com.penny.wallet.ui.components.StepperRow
import com.penny.wallet.ui.components.SwitchRow

internal fun LazyListScope.quickAddSection(x: PrefsCtx, accounts: List<Account>) {
    val p = x.p
    val d = x.d
    settingsCard("quickAdd", "Quick add") {
        ChoiceRow(
            "Default type", listOf(QuickType.EXPENSE to "Expense", QuickType.INCOME to "Income"), p.quickAddDefaultType,
            { v -> x.set { it.copy(quickAddDefaultType = v) } },
            onReset = x.reset(p.quickAddDefaultType, d.quickAddDefaultType) { it.copy(quickAddDefaultType = d.quickAddDefaultType) },
        )
        // A fixed account that was archived/deleted falls back to "Last used" in the picker.
        val fixed = p.fixedAccountId?.takeIf { id -> accounts.any { it.id == id } }
        ChoiceRow(
            "Default account", listOf<Pair<String?, String>>(null to "Last used") + accounts.map { it.id to it.name }, fixed,
            { v -> x.set { it.copy(fixedAccountId = v) } },
            onReset = x.reset(p.fixedAccountId, d.fixedAccountId) { it.copy(fixedAccountId = d.fixedAccountId) },
        )
        ChoiceRow(
            "Category chip order",
            listOf(ChipOrder.MOST_USED to "Most used", ChipOrder.MANUAL to "Manual", ChipOrder.ALPHABETICAL to "A–Z"), p.chipOrder,
            { v -> x.set { it.copy(chipOrder = v) } }, onReset = x.reset(p.chipOrder, d.chipOrder) { it.copy(chipOrder = d.chipOrder) },
        )
        SwitchRow(
            "Save on chip tap", p.saveOnChipTap, { v -> x.set { it.copy(saveOnChipTap = v) } },
            subtitle = "Tapping a category saves immediately",
            onReset = x.reset(p.saveOnChipTap, d.saveOnChipTap) { it.copy(saveOnChipTap = d.saveOnChipTap) },
        )
        SwitchRow(
            "Show note field", p.showNoteField, { v -> x.set { it.copy(showNoteField = v) } },
            onReset = x.reset(p.showNoteField, d.showNoteField) { it.copy(showNoteField = d.showNoteField) },
        )
    }
}

internal fun LazyListScope.chartsSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("charts", "Expenses & charts") {
        ChoiceRow(
            "Default chart", listOf(ChartStyle.DONUT to "Donut", ChartStyle.BARS to "Bars"), p.defaultChart,
            { v -> x.set { it.copy(defaultChart = v) } }, onReset = x.reset(p.defaultChart, d.defaultChart) { it.copy(defaultChart = d.defaultChart) },
        )
        ChoiceRow(
            "Months in history", listOf(3 to "3", 6 to "6", 12 to "12"), p.historyMonths,
            { v -> x.set { it.copy(historyMonths = v) } }, onReset = x.reset(p.historyMonths, d.historyMonths) { it.copy(historyMonths = d.historyMonths) },
        )
        ChoiceRow(
            "First day of week", listOf(1 to "Monday", 7 to "Sunday"), p.firstDayOfWeek,
            { v -> x.set { it.copy(firstDayOfWeek = v) } },
            onReset = x.reset(p.firstDayOfWeek, d.firstDayOfWeek) { it.copy(firstDayOfWeek = d.firstDayOfWeek) },
        )
        StepperRow(
            "Month start day", p.monthStartDay, 1..28, { v -> x.set { it.copy(monthStartDay = v) } },
            format = { ordinal(it) },
            subtitle = if (p.monthStartDay == 1) "e.g. payday on the 27th → month runs 27th–26th"
            else "Month runs ${ordinal(p.monthStartDay)}–${ordinal(p.monthStartDay - 1)}",
            onReset = x.reset(p.monthStartDay, d.monthStartDay) { it.copy(monthStartDay = d.monthStartDay) },
        )
    }
}

internal fun LazyListScope.savingsSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("savings", "Savings") {
        ChoiceRow(
            "Goal layout", listOf(GoalLayout.GRID to "Grid", GoalLayout.LIST to "List"), p.goalLayout,
            { v -> x.set { it.copy(goalLayout = v) } }, onReset = x.reset(p.goalLayout, d.goalLayout) { it.copy(goalLayout = d.goalLayout) },
        )
        ChoiceRow(
            "Sort goals by", listOf(GoalSort.PROGRESS to "Progress", GoalSort.DEADLINE to "Deadline", GoalSort.MANUAL to "Manual"), p.goalSort,
            { v -> x.set { it.copy(goalSort = v) } }, onReset = x.reset(p.goalSort, d.goalSort) { it.copy(goalSort = d.goalSort) },
        )
    }
}

internal fun ordinal(n: Int): String {
    val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "$n$suffix"
}
