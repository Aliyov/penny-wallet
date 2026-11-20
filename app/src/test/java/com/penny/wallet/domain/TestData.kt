package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate

private var seq = 0
fun d(s: String): LocalDate = LocalDate.parse(s)

fun t(type: TxType, amount: Long, acc: String, date: String = "2026-11-10", to: String? = null,
      cat: String? = null, goal: String? = null) =
    Transaction("t${seq++}", type, amount, acc, to, cat, goal, null, d(date), null, seq.toLong())

fun rule(freq: Frequency = Frequency.MONTHLY, dom: Int? = 14, month: Int? = null, dow: Int? = null,
         next: String = "2026-11-14", before: Int = 1, auto: Boolean = false, active: Boolean = true, amount: Long = 1000) =
    RecurringExpense("r", "Phone", amount, "c", "a", freq, dom, month, dow, d(next), before, auto, active)
