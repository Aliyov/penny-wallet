package com.penny.wallet.feature.support

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.EmojiFoodBeverage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.Celebration
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.data.billing.PlayTipRepository
import com.penny.wallet.data.billing.Tip
import com.penny.wallet.data.billing.TipEvent
import com.penny.wallet.data.billing.TipKind
import com.penny.wallet.data.billing.TipRepository
import com.penny.wallet.data.billing.TipsState

class SupportViewModel(val tips: TipRepository) : ViewModel() {
    override fun onCleared() = tips.close()
}

@Composable
fun SupportScreen(onBack: () -> Unit) {
    val vm = pennyViewModel { SupportViewModel(PlayTipRepository(it.context)) }
    val state by vm.tips.state.collectAsStateWithLifecycle()
    val c = Penny.colors
    val snack = LocalSnack.current
    val context = LocalContext.current
    var thanks by rememberSaveable { mutableStateOf(false) }
    val pendingMsg = stringResource(R.string.support_pending)
    val failedMsg = stringResource(R.string.support_failed)

    LaunchedEffect(vm) {
        vm.tips.events.collect { e ->
            when (e) {
                TipEvent.THANKS -> thanks = true
                TipEvent.PENDING -> snack.show(pendingMsg)
                TipEvent.FAILED -> snack.show(failedMsg)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = c.textPrimary) }
            Text(stringResource(R.string.support_title), style = Penny.type.title, color = c.textPrimary)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Penny.dims.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            Text(stringResource(R.string.support_body), style = Penny.type.body, color = c.textSecondary)
            if (thanks) {
                PennyCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.support_thanks), style = Penny.type.bodyStrong, color = c.accent)
                }
            }
            when (val s = state) {
                TipsState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                TipsState.Unavailable -> PennyCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.support_unavailable), style = Penny.type.body, color = c.textSecondary)
                }
                is TipsState.Ready -> s.tips.forEach { tip ->
                    TipRow(tip) { context.findActivity()?.let { vm.tips.buy(it, tip.kind) } }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    Celebration(visible = thanks) {}
}

@Composable
private fun TipRow(tip: Tip, onClick: () -> Unit) {
    val c = Penny.colors
    val (icon, name) = when (tip.kind) {
        TipKind.TEA -> Icons.Rounded.EmojiFoodBeverage to stringResource(R.string.support_tip_tea)
        TipKind.CAPPUCCINO -> Icons.Rounded.Coffee to stringResource(R.string.support_tip_cappuccino)
    }
    PennyCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = c.accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Text(name, style = Penny.type.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
            Text(tip.price, style = Penny.type.bodyStrong, color = c.accent)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
