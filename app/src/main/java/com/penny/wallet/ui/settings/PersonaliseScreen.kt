package com.penny.wallet.ui.settings

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.ui.components.PennyCard
import com.penny.wallet.ui.components.SectionLabel
import com.penny.wallet.ui.pennyViewModel
import com.penny.wallet.ui.theme.Penny

/** Settings → Personalise (SPEC §7a). Every change is written immediately and applied live. */
@Composable
fun PersonaliseScreen(onBack: () -> Unit) {
    val vm = pennyViewModel { PersonaliseViewModel(it) }
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val c = Penny.colors
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val ctx = PrefsCtx(prefs, vm.defaults, vm::set)

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.textPrimary) }
            Text("Personalise", style = Penny.type.title, color = c.textPrimary, modifier = Modifier.weight(1f))
            TextButton(onClick = { confirmReset = true }) { Text("Reset all", style = Penny.type.label, color = c.accent) }
        }
        val reorder = rememberHomeReorderState(listState, ctx)
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = Penny.dims.gutter).navigationBarsPadding(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            appearanceSection(ctx)
            homeSection(ctx, reorder)
            quickAddSection(ctx, accounts.filter { !it.archived })
            chartsSection(ctx)
            savingsSection(ctx)
            remindersSection(ctx)
            backupSection(ctx)
            securitySection(ctx)
            item("bottom") { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = c.surface2,
            title = { Text("Reset all options?", style = Penny.type.title, color = c.textPrimary) },
            text = { Text("Every Personalise option goes back to its default. Your data and backup folder are kept.", style = Penny.type.body, color = c.textSecondary) },
            confirmButton = { TextButton(onClick = { vm.resetAll(); confirmReset = false }) { Text("Reset", color = c.danger) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = c.textSecondary) } },
        )
    }
}

/** Current prefs, defaults and the writer, passed to every section. */
internal class PrefsCtx(val p: UserPrefs, val d: UserPrefs, private val setter: ((UserPrefs) -> UserPrefs) -> Any) {
    fun set(t: (UserPrefs) -> UserPrefs) { setter(t) }

    /** Reset action for a row, or null when [current] already equals the default. */
    fun <T> reset(current: T, default: T, t: (UserPrefs) -> UserPrefs): (() -> Unit)? =
        if (current != default) ({ set(t) }) else null
}

/** A section label followed by one card holding its rows. */
internal fun LazyListScope.settingsCard(key: String, title: String, content: @Composable () -> Unit) {
    item(key) {
        Column {
            SectionLabel(title, Modifier.padding(top = 8.dp))
            PennyCard(Modifier.fillMaxWidth()) { content() }
        }
    }
}
