package com.penny.wallet.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.clickableNoRipple
import com.penny.wallet.core.ui.theme.Penny

@Composable
internal fun WelcomeCard(onAddAccount: () -> Unit, onTrySample: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    PennyCard(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.welcome_title), style = Penny.type.title, color = c.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.welcome_body), style = Penny.type.body, color = c.textSecondary)
        Spacer(Modifier.height(18.dp))
        PennyButton(stringResource(R.string.welcome_add_account), onAddAccount, Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.welcome_try_sample), style = Penny.type.label, color = c.accent,
            modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)
                .clickableNoRipple(onTrySample).padding(top = 16.dp, start = 12.dp, end = 12.dp),
        )
    }
}

@Composable
internal fun SampleBanner(onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    Row(
        modifier.fillMaxWidth().clip(Penny.dims.smallShape).background(c.accent.copy(alpha = 0.12f))
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.sample_banner), style = Penny.type.label, color = c.textPrimary, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
        Text(
            stringResource(R.string.sample_remove), style = Penny.type.label, color = c.accent,
            modifier = Modifier.clickableNoRipple(onRemove).padding(horizontal = 14.dp, vertical = 14.dp),
        )
    }
}
