package com.penny.wallet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.penny.wallet.feature.lock.LockGate
import com.penny.wallet.feature.nav.PennyRoot
import com.penny.wallet.core.ui.theme.PennyTheme

// AppCompatActivity (a FragmentActivity) so BiometricPrompt works.
class MainActivity : AppCompatActivity() {
    /** Incremented for each "open Quick add" deep link (widget); PennyRoot reacts to changes. */
    private val quickAddRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        val c = container
        // Hold the splash until settings are read so the user's accent/theme never flashes the defaults.
        installSplashScreen().setKeepOnScreenCondition { !c.settings.isLoaded }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (savedInstanceState == null) handle(intent)
        setContent {
            val prefs by c.settings.prefs.collectAsStateWithLifecycle()
            PennyTheme(prefs) { LockGate { PennyRoot(c, quickAddRequests.intValue) } }
        }
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
