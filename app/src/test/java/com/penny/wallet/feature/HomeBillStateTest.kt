package com.penny.wallet.feature

import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.domain.BillDetector
import com.penny.wallet.domain.d
import com.penny.wallet.domain.t
import com.penny.wallet.feature.home.buildHomeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeBillStateTest {
    private val acc = listOf(Account("bank", "Bank", "bank", sortOrder = 0, createdAt = 0))
    private val txs = listOf("2026-09-14", "2026-10-14", "2026-11-14").map { t(TxType.EXPENSE, 12_99, "bank", date = it, cat = "subs").copy(note = "Netflix") }

    private val subs = Category("subs", "Subscriptions", "other", 0, sortOrder = 0)

    private fun home(prefs: UserPrefs = UserPrefs(), sample: Boolean = false, cats: List<Category> = listOf(subs)) =
        buildHomeState(acc, cats, emptyList(), txs, emptyList(), prefs, sample, d("2026-11-20"), 0)

    @Test fun suggestionShowsOnHome() = assertEquals("Netflix", home().billSuggestion?.name)

    @Test fun dismissedSuggestionIsGone() = assertNull(home(UserPrefs(dismissedBills = setOf(BillDetector.key("Netflix", "subs")))).billSuggestion)

    @Test fun sampleDataNeverSuggests() = assertNull(home(sample = true).billSuggestion)

    @Test fun archivedCategoryIsNotSuggested() = assertNull(home(cats = listOf(subs.copy(archived = true))).billSuggestion)
}
