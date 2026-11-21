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
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.penny.wallet.feature.lock.LockGate
import com.penny.wallet.feature.nav.PennyRoot
import com.penny.wallet.core.ui.theme.PennyTheme
import com.penny.wallet.core.ui.theme.isDarkTheme

// AppCompatActivity (a FragmentActivity) so BiometricPrompt works.
class MainActivity : AppCompatActivity() {
    // Bumped per widget "open Quick add" tap so repeated taps still reach PennyRoot.
    private val quickAddRequests = mutableIntStateOf(0)

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
            PennyTheme(prefs) { LockGate { PennyRoot(c, quickAddRequests.intValue) } }
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
        if (i?.action == ACTION_QUICK_ADD) quickAddRequests.intValue++
    }

    companion object { const val ACTION_QUICK_ADD = "com.penny.wallet.QUICK_ADD" }
}
