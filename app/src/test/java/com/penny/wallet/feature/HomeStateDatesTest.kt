package com.penny.wallet.feature

import com.penny.wallet.data.db.LargeData
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.feature.expenses.Filters
import com.penny.wallet.feature.expenses.buildExpensesState
import com.penny.wallet.feature.home.buildHomeState
import org.junit.Test
import java.time.LocalDate

/** Home and Expenses must build on every calendar day, for every month start day, with and without data. */
class HomeStateDatesTest {
    @Test fun everyDayAndStartDay() {
        val failures = mutableListOf<String>()
        var day = LocalDate.of(2024, 1, 1)
        while (day.isBefore(LocalDate.of(2029, 1, 1))) {
            for (start in 1..28) {
                val prefs = UserPrefs(monthStartDay = start)
                for (data in listOf(null, LargeData(count = 300, today = day))) {
                    runCatching {
                        buildHomeState(data?.accounts.orEmpty(), data?.categories.orEmpty(), data?.goals.orEmpty(),
                            data?.transactions.orEmpty(), data?.recurring.orEmpty(), prefs, false, day, 0)
                        buildExpensesState(data?.transactions.orEmpty(), data?.categories.orEmpty(), data?.accounts.orEmpty(),
                            prefs, Filters(null, null, ""), "-", day)
                    }.onFailure { if (failures.size < 20) failures += "$day start=$start data=${data != null}: $it" }
                }
            }
            day = day.plusDays(1)
        }
        check(failures.isEmpty()) { failures.joinToString("\n") }
    }
}
