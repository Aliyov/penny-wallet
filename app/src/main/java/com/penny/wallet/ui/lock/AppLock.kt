package com.penny.wallet.ui.lock

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.penny.wallet.data.prefs.UserPrefs

/**
 * Process-wide lock state. Locked at cold start; after going to the background it re-locks once
 * the "lock after" delay has passed (SPEC §7a Security).
 */
object AppLock : DefaultLifecycleObserver {
    var locked by mutableStateOf(true)
        private set
    private var backgroundedAt: Long? = null
    private var prefsSource: () -> UserPrefs = { UserPrefs() }

    fun install(currentPrefs: () -> UserPrefs) {
        prefsSource = currentPrefs
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) { backgroundedAt = SystemClock.elapsedRealtime() }

    override fun onStart(owner: LifecycleOwner) {
        val at = backgroundedAt ?: return
        backgroundedAt = null
        val p = prefsSource()
        if (p.biometricLock && SystemClock.elapsedRealtime() - at >= p.lockAfter.millis) locked = true
    }

    fun unlock() { locked = false }
}
