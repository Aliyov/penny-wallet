package com.penny.wallet.data.repo

import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.testDb
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.domain.BillDetector
import com.penny.wallet.domain.BillSuggestion
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.domain.DueState
import com.penny.wallet.domain.Recurrence
import com.penny.wallet.domain.d
import com.penny.wallet.domain.t
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BillSuggestionRepoTest {
    private val db = testDb()
    private val repo = RecurringRepo(db) {}

    @After fun close() = db.close()

    private fun suggestion(day: Int, last: String, ids: List<String> = emptyList()) =
        BillSuggestion(BillDetector.key("Netflix", "subs"), "Netflix", "subs", "bank", 12_99, day, 3, d(last), ids)

    @Suppress("UNUSED_PARAMETER") // [today] documents the scenario; the due date depends only on the last payment
    private fun add(day: Int, last: String, today: String) = runBlocking { repo.addFromSuggestion(suggestion(day, last), 2) }

    @Test fun createsAMonthlyHandPaidBill() {
        val r = add(14, "2026-11-14", "2026-11-20")
        assertEquals("Netflix", r.name)
        assertEquals(12_99L, r.amountCents)
        assertEquals(Frequency.MONTHLY, r.frequency)
        assertEquals(14, r.dayOfMonth)
        assertEquals(2, r.remindDaysBefore)
        assertFalse(r.autoPay)
        assertEquals(d("2026-12-14"), r.nextDueDate)
        assertEquals(listOf(r), runBlocking { repo.all() })
    }

    @Test fun laterDayThisMonthIsStillDue() = assertEquals(d("2026-11-25"), add(25, "2026-10-25", "2026-11-20").nextDueDate)

    @Test fun paidTodayIsDueNextMonth() = assertEquals(d("2026-12-20"), add(20, "2026-11-20", "2026-11-20").nextDueDate)

    @Test fun paidEarlyThisMonthIsNotDueAgainDaysLater() = assertEquals(d("2026-12-31"), add(31, "2026-11-28", "2026-11-29").nextDueDate)

    @Test fun day31ClampsInShortMonths() = assertEquals(d("2027-02-28"), add(31, "2027-01-31", "2027-02-01").nextDueDate)

    @Test fun missedPaymentShowsAsOverdue() {
        val r = add(1, "2026-10-01", "2026-11-05")
        assertEquals(d("2026-11-01"), r.nextDueDate)
        assertEquals(DueState.OVERDUE, Recurrence.state(r, d("2026-11-05")))
    }

    @Test fun doubleTapAddsOnce() {
        val a = add(14, "2026-11-14", "2026-11-20")
        val b = add(14, "2026-11-14", "2026-11-20")
        assertEquals(a, b)
        assertEquals(1, runBlocking { repo.all() }.size)
    }

    @Test fun sourcePaymentsAreLinkedSoARenameNeverBringsItBack() = runBlocking {
        val acc = db.accounts().getAll().firstOrNull() ?: com.penny.wallet.data.db.entities.Account("bank", "Bank", "bank", sortOrder = 0, createdAt = 0).also { db.accounts().upsert(it) }
        val txs = listOf("2026-09-14", "2026-10-14", "2026-11-14").map { t(TxType.EXPENSE, 12_99, acc.id, date = it, cat = "subs").copy(note = "Netflix") }
        db.transactions().upsertAll(txs)
        val s = BillDetector.suggestions(txs, emptyList(), emptySet(), d("2026-11-20")).single()
        val rule = repo.addFromSuggestion(s, 1)
        repo.save(rule.copy(name = "Netflix Premium"))
        val stored = db.transactions().getAll()
        assertTrue(stored.all { it.recurringId == rule.id })
        assertTrue(BillDetector.suggestions(stored, repo.all(), emptySet(), d("2026-11-20")).isEmpty())
    }

    @Test fun dismissalsSurviveSettingsJson() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val p = UserPrefs(dismissedBills = setOf("netflix|subs"))
        assertEquals(p, json.decodeFromString(UserPrefs.serializer(), json.encodeToString(UserPrefs.serializer(), p)))
        assertEquals(emptySet<String>(), json.decodeFromString(UserPrefs.serializer(), "{}").dismissedBills)
    }
}
