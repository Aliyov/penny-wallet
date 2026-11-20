package com.penny.wallet

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.ui.nav.PennyRoot
import com.penny.wallet.ui.theme.PennyTheme

// AppCompatActivity (a FragmentActivity) so BiometricPrompt works in phase 11.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val c = container
        setContent {
            val prefs by c.settings.prefs.collectAsStateWithLifecycle()
            PennyTheme(prefs) { PennyRoot(c) }
        }
    }
}
