package com.penny.wallet.feature.year

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.R
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.domain.YearOverview
import com.penny.wallet.domain.YearOverviews
import com.penny.wallet.feature.expenses.CategorySlice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class YearState(
    val year: Int = LocalDate.now().year,
    val overview: YearOverview? = null,
    val years: List<Int> = emptyList(),
    val categories: List<CategorySlice> = emptyList(),
    val canGoNewer: Boolean = false,
    val canGoOlder: Boolean = false,
    val loading: Boolean = true,
)

class YearViewModel(private val c: AppContainer) : ViewModel() {
    /** null = the year of the current budgeting period. */
    private val selected = MutableStateFlow<Int?>(null)

    val state: StateFlow<YearState> = combine(c.transactions.observe(), c.categories.observe(), c.settings.prefs, selected) { txs, cats, prefs, sel ->
        val today = LocalDate.now()
        val year = sel ?: MoneyCalculator.periodOf(today, prefs.monthStartDay).label.year
        val years = YearOverviews.years(txs, prefs.monthStartDay, today)
        val overview = YearOverviews.of(txs, year, prefs.monthStartDay, today)
        YearState(
            year = year, overview = overview, years = years,
            categories = resolve(overview, cats, c.context.getString(R.string.expenses_uncategorised)),
            canGoNewer = years.any { it > year }, canGoOlder = years.any { it < year }, loading = false,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearState())

    fun prevYear() = step(older = true)
    fun nextYear() = step(older = false)

    private fun step(older: Boolean) {
        val s = state.value
        val target = if (older) s.years.filter { it < s.year }.maxOrNull() else s.years.filter { it > s.year }.minOrNull()
        if (target != null) selected.value = target
    }
}

private fun resolve(o: YearOverview, cats: List<Category>, uncategorised: String): List<CategorySlice> {
    val byId = cats.associateBy { it.id }
    return o.topCategories.map { t ->
        val cat = t.categoryId?.let(byId::get)
        CategorySlice(t.categoryId, cat?.name ?: uncategorised, cat?.icon ?: "other", cat?.color ?: 0xFF8E8E93, t.spent, t.share)
    }
}
