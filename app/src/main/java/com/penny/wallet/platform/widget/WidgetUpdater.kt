package com.penny.wallet.platform.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.penny.wallet.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Refreshes widgets after every DB write (debounced) and when a widget-relevant setting changes. */
class WidgetUpdater(private val context: Context, private val c: AppContainer) {
    private var pending: Job? = null

    fun start() {
        c.onDataChanged = { request() }
        c.appScope.launch {
            c.settings.prefs.map { listOf(it.accentArgb, it.hideAmounts, it.background, it.showCents, it.numberFormat, it.symbolBefore, it.monthStartDay) }
                .distinctUntilChanged().collect { request() }
        }
    }

    /** Coalesces bursts (e.g. a restore or several auto-payments) into one update. */
    fun request() {
        pending?.cancel()
        pending = c.appScope.launch {
            delay(400)
            runCatching { BalanceWidget().updateAll(context) }
            runCatching { QuickAddWidget().updateAll(context) }
        }
    }
}
