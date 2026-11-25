package com.penny.wallet.feature.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.penny.wallet.container
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.ui.theme.glow

private const val AUTH = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/** True if the phone can verify the user (fingerprint/face or PIN/pattern). */
fun canLock(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

fun promptUnlock(activity: FragmentActivity, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
    })
    prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(activity.getString(R.string.lock_prompt_title)).setAllowedAuthenticators(AUTH).build())
}

/**
 * Covers the app while locked. If the phone has no lock method at all, it never locks the user out.
 * [canPrompt] false holds the system unlock prompt back (while the opening animation plays).
 */
@Composable
fun LockGate(canPrompt: Boolean = true, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    // Until settings are read, prefs are defaults (lock off): show nothing rather than leak content.
    // The splash screen covers this moment.
    val loaded by context.container.settings.loaded.collectAsState()
    if (!loaded) return
    val mustLock = Penny.prefs.biometricLock && AppLock.locked && canLock(activity)
    if (!mustLock) {
        // Lock disabled (or impossible on this phone): drop the cold-start lock so enabling it later
        // doesn't immediately lock the user out mid-session.
        if (AppLock.locked) LaunchedEffect(Unit) { AppLock.unlock() }
        content()
        return
    }
    LaunchedEffect(canPrompt) { if (canPrompt) promptUnlock(activity) { AppLock.unlock() } }
    val c = Penny.colors
    Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(84.dp).glow(c.accent, Penny.motion.glowAlpha), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lock, null, tint = c.accent, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.lock_locked), style = Penny.type.title, color = c.textPrimary)
            Spacer(Modifier.height(24.dp))
            PennyButton(stringResource(R.string.lock_unlock), onClick = { promptUnlock(activity) { AppLock.unlock() } })
        }
    }
}
