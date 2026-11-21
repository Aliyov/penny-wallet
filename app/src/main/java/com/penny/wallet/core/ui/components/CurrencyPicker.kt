package com.penny.wallet.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.currentLocale
import java.util.Currency
import java.util.Locale

private val COMMON = listOf("EUR", "USD", "GBP", "CHF", "PLN", "CZK", "SEK", "NOK", "DKK", "HUF", "RON", "BGN", "TRY", "CAD", "AUD", "JPY")

fun currencyLabel(code: String, locale: Locale): String {
    val cur = Formatters.currency(code)
    val sym = cur.getSymbol(locale)
    val name = cur.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
    return if (sym == code) "$code · $name" else "$code · $name ($sym)"
}

/** Common currencies first, then every ISO currency the device knows, searchable by code or name. */
@Composable
fun CurrencyPickerSheet(selected: String, onDismiss: () -> Unit, onPick: (String) -> Unit, note: String? = null) {
    val c = Penny.colors
    val locale = currentLocale()
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember(locale) {
        val rest = Currency.getAvailableCurrencies().map { it.currencyCode }
            .filter { it !in COMMON && Formatters.currency(it).defaultFractionDigits >= 0 }
            .sortedBy { Formatters.currency(it).getDisplayName(locale) }
        (COMMON + rest).map { it to currencyLabel(it, locale) }
    }
    val shown = remember(all, query) {
        val q = query.trim()
        if (q.isEmpty()) all else all.filter { it.second.contains(q, ignoreCase = true) }
    }
    PennySheet(onDismiss = onDismiss, title = stringResource(R.string.currency_title)) {
        if (note != null) Text(note, style = Penny.type.caption, color = c.textSecondary, modifier = Modifier.padding(bottom = 10.dp))
        val shape = Penny.dims.smallShape
        BasicTextField(
            query, { query = it.take(30) }, singleLine = true,
            textStyle = Penny.type.body.copy(color = c.textPrimary), cursorBrush = SolidColor(c.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().clip(shape).background(c.surface1).border(0.75.dp, c.hairline, shape).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    if (query.isEmpty()) Text(stringResource(R.string.currency_search), style = Penny.type.body, color = c.textSecondary)
                    inner()
                }
            },
        )
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(top = 8.dp)) {
            items(shown, key = { it.first }) { (code, label) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape).clickable { onPick(code) }.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = Penny.type.body, color = c.textPrimary, modifier = Modifier.weight(1f))
                    if (code == selected) {
                        Box(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Check, stringResource(R.string.a11y_selected), tint = c.accent)
                    }
                }
            }
        }
    }
}
