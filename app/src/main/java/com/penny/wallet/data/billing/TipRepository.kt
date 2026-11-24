package com.penny.wallet.data.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class TipKind(val productId: String) { TEA("tip_tea"), CAPPUCCINO("tip_cappuccino") }

data class Tip(val kind: TipKind, val price: String)

sealed interface TipsState {
    data object Loading : TipsState
    /** Play Billing missing or not set up (e.g. an APK from GitHub, or products not live yet). */
    data object Unavailable : TipsState
    data class Ready(val tips: List<Tip>) : TipsState
}

enum class TipEvent { THANKS, PENDING, FAILED }

/** One-off tips. A tip unlocks nothing; purchases are consumed so the same tip can be given again. */
interface TipRepository {
    val state: StateFlow<TipsState>
    val events: Flow<TipEvent>
    fun buy(activity: Activity, kind: TipKind)
    fun close()
}
