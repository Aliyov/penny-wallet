package com.penny.wallet.feature.importcsv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.ButtonKind
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.PennyChip
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.components.SwitchRow
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny

private val CSV_TYPES = arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/csv", "application/vnd.ms-excel", "*/*")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportScreen(onBack: () -> Unit) {
    val vm = pennyViewModel { ImportViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val res = LocalResources.current
    val c = Penny.colors
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::load) }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = c.textPrimary) }
            Text(stringResource(R.string.import_title), style = Penny.type.title, color = c.textPrimary)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Penny.dims.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            if (!s.loaded) Text(stringResource(R.string.import_intro), style = Penny.type.body, color = c.textSecondary)
            s.loadError?.let { Text(it, style = Penny.type.label, color = c.danger) }
            if (s.loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.import_loading), style = Penny.type.label, color = c.textSecondary)
                }
            }
            PennyButton(
                stringResource(if (s.loaded) R.string.import_choose_other else R.string.import_choose), { pick.launch(CSV_TYPES) },
                Modifier.fillMaxWidth(), kind = if (s.loaded) ButtonKind.SECONDARY else ButtonKind.PRIMARY, enabled = !s.loading && !s.busy,
            )
            if (s.loaded) {
                SummaryCard(s)
                Warnings(s)

                SectionLabel(stringResource(R.string.import_section_columns), Modifier.padding(top = 8.dp))
                PennyCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        s.columns.forEach { col -> ColumnRoleRow(col) { vm.setColumn(col.index, it) } }
                    }
                }

                if (s.accounts.size > 1) {
                    SectionLabel(stringResource(R.string.import_section_account), Modifier.padding(top = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.accounts.forEach { a -> PennyChip(a.name, a.id == s.accountId, { vm.setAccount(a.id) }) }
                    }
                }
                FallbackCategoryRow(s.categories, s.fallbackCategoryId, vm::setFallbackCategory)
                SwitchRow(stringResource(R.string.import_create_missing), s.createMissing, vm::setCreateMissing,
                    subtitle = stringResource(R.string.import_create_missing_body))
                SwitchRow(stringResource(R.string.import_skip_duplicates), s.skipDuplicates, vm::setSkipDuplicates,
                    subtitle = stringResource(R.string.import_skip_duplicates_body))

                if (s.rows.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.import_section_preview), Modifier.padding(top = 8.dp))
                    PreviewList(s.rows)
                }
                ErrorList(s)

                PennyButton(
                    pluralStringResource(R.plurals.import_button, s.importCount, s.importCount),
                    {
                        vm.import(
                            onDone = { last ->
                                snack.undo(res.getQuantityString(R.plurals.import_done, last.count, last.count)) { vm.undo(last) }
                                onBack()
                            },
                            onError = { snack.show(it) },
                        )
                    },
                    Modifier.fillMaxWidth().padding(top = 8.dp), enabled = s.canImport,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
