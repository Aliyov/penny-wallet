package com.penny.wallet.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.ButtonKind
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.domain.BillSuggestion

/** "Looks like a bill": a repeating expense the user can turn into a bill or dismiss for good. */
@Composable
internal fun BillSuggestionCard(bill: BillSuggestion, onAdd: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    PennyCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.EventRepeat, null, tint = c.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.home_bill_title), style = Penny.type.caption, color = c.textSecondary)
                Text(stringResource(R.string.home_bill_name, bill.name, money(bill.amountCents)), style = Penny.type.bodyStrong, color = c.textPrimary)
                Text(pluralStringResource(R.plurals.home_bill_body, bill.occurrences, bill.occurrences, bill.dayOfMonth), style = Penny.type.caption, color = c.textSecondary)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PennyButton(stringResource(R.string.home_bill_dismiss), onDismiss, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            PennyButton(stringResource(R.string.home_bill_add), onAdd, Modifier.weight(1f))
        }
    }
}
