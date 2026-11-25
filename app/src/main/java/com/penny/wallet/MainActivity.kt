package com.penny.wallet

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.penny.wallet.feature.lock.LockGate
import com.penny.wallet.feature.nav.LaunchRequest
import com.penny.wallet.feature.nav.PennyRoot
import com.penny.wallet.core.ui.theme.PennyTheme
import com.penny.wallet.core.ui.theme.isDarkTheme

// AppCompatActivity (a FragmentActivity) so BiometricPrompt works.
class MainActivity : AppCompatActivity() {
    // Set by a widget, shortcut or tile tap and cleared once PennyRoot acted on it. Held here, not in composition,
    // because the app lock removes PennyRoot while locked and would otherwise forget or replay it.
    private val pendingLaunch = mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val c = container
        // Hold the splash until settings are read so the user's accent/theme never flashes the defaults.
        installSplashScreen().setKeepOnScreenCondition { !c.settings.isLoaded }
        super.onCreate(savedInstanceState)
        applySystemBars((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
        if (savedInstanceState == null) handle(intent)
        setContent {
            val prefs by c.settings.prefs.collectAsStateWithLifecycle()
            val dark = isDarkTheme(prefs.themeMode)
            LaunchedEffect(dark) { applySystemBars(dark) }
            PennyTheme(prefs) { LockGate { PennyRoot(c, pendingLaunch.value) { pendingLaunch.value = null } } }
        }
    }

    private fun applySystemBars(dark: Boolean) {
        val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        val scan = when (i?.action) {
            ACTION_QUICK_ADD -> false
            ACTION_SCAN_RECEIPT -> true
            else -> return
        }
        pendingLaunch.value = LaunchRequest(scan)
    }

    companion object {
        const val ACTION_QUICK_ADD = "com.penny.wallet.QUICK_ADD"
        const val ACTION_SCAN_RECEIPT = "com.penny.wallet.SCAN_RECEIPT"
    }
}
