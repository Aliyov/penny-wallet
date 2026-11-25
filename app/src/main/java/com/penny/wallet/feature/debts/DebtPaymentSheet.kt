package com.penny.wallet.feature.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.AmountSheet
import com.penny.wallet.core.ui.components.PennyChip
import com.penny.wallet.core.ui.components.SwitchRow
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.data.db.entities.Account

/** A repayment, optionally also recorded as money moving in one of the accounts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaymentSheet(
    remaining: Long,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (amount: Long, recordInAccountId: String?) -> Unit,
) {
    var record by rememberSaveable { mutableStateOf(false) }
    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id) }
    AmountSheet(
        stringResource(R.string.debts_record_payment), onDismiss = onDismiss, confirmLabel = stringResource(R.string.action_save),
        initialCents = remaining,
        extra = if (accounts.isEmpty()) null else {
            {
                SwitchRow(stringResource(R.string.debts_record_in_account), record, { record = it })
                if (record) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.forEach { a -> PennyChip(a.name, a.id == accountId, { accountId = a.id }) }
                    }
                    Text(stringResource(R.string.debts_record_hint), style = Penny.type.caption, color = Penny.colors.textSecondary)
                }
            }
        },
    ) { cents -> onSave(cents, if (record) accountId else null) }
}
