package com.penny.wallet.core.ui.common

import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.domain.DueState
import com.penny.wallet.domain.Recurrence
import java.time.LocalDate

data class UpcomingItem(val rule: RecurringExpense, val state: DueState, val icon: String, val color: Long?, val categoryName: String)

fun upcomingItems(rules: List<RecurringExpense>, categories: List<Category>, today: LocalDate, days: Long = 7): List<UpcomingItem> {
    val cat = categories.associateBy { it.id }
    return Recurrence.upcoming(rules, today, days).map { r ->
        val c = cat[r.categoryId]
        UpcomingItem(r, Recurrence.state(r, today), c?.icon ?: "subscription", c?.color, c?.name ?: "")
    }
}
