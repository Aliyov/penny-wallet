package com.penny.wallet.feature.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.theme.Penny

/** "Your data stays here": what the app does and doesn't do with the user's data. Every line must stay true. */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val c = Penny.colors
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = c.textPrimary) }
            Text(stringResource(R.string.privacy_title), style = Penny.type.title, color = c.textPrimary)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Penny.dims.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            Text(stringResource(R.string.privacy_intro), style = Penny.type.body, color = c.textSecondary)
            Point(Icons.Rounded.WifiOff, R.string.privacy_offline_title, R.string.privacy_offline_body)
            Point(Icons.Rounded.PhoneAndroid, R.string.privacy_local_title, R.string.privacy_local_body)
            Point(Icons.Rounded.Backup, R.string.privacy_backup_title, R.string.privacy_backup_body)
            Point(Icons.Rounded.Receipt, R.string.privacy_receipts_title, R.string.privacy_receipts_body)
            Point(Icons.Rounded.BugReport, R.string.privacy_crash_title, R.string.privacy_crash_body)
            Point(Icons.Rounded.Favorite, R.string.privacy_tips_title, R.string.privacy_tips_body)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Point(icon: ImageVector, title: Int, body: Int) {
    val c = Penny.colors
    PennyCard(Modifier.fillMaxWidth()) {
        Row {
            Icon(icon, null, tint = c.accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), style = Penny.type.bodyStrong, color = c.textPrimary)
                Text(stringResource(body), style = Penny.type.body, color = c.textSecondary)
            }
        }
    }
}
