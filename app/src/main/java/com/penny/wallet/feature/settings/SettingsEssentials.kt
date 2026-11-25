package com.penny.wallet.feature.settings

import com.penny.wallet.core.ui.components.LocalSnack
import java.time.LocalDate
import com.penny.wallet.domain.MoneyCalculator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.Build
import android.Manifest
import com.penny.wallet.container
import com.penny.wallet.platform.AppLanguage
import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import com.penny.wallet.core.money.Formatters
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.ui.graphics.Color
import com.penny.wallet.core.ui.components.AmountSheet
import com.penny.wallet.core.ui.components.ChoiceRow
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.ItemPalette
import com.penny.wallet.core.ui.components.CurrencyPickerSheet
import com.penny.wallet.core.ui.components.NavRow
import com.penny.wallet.core.ui.components.StepperRow
import com.penny.wallet.core.ui.components.SwitchRow
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.data.prefs.LockAfter
import com.penny.wallet.data.prefs.ThemeMode
import com.penny.wallet.core.util.currentLocale
import com.penny.wallet.feature.lock.canLock
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun ThemeSetting(x: PrefsCtx) {
    ChoiceRow(
        stringResource(R.string.theme_title),
        listOf(
            ThemeMode.SYSTEM to stringResource(R.string.theme_system), ThemeMode.LIGHT to stringResource(R.string.theme_light),
            ThemeMode.DARK to stringResource(R.string.theme_dark),
        ),
        x.p.themeMode, { v -> x.set { it.copy(themeMode = v) } },
    )
}

@Composable
internal fun LanguageSetting() {
    if (!AppLanguage.supported) return
    val context = LocalContext.current
    var tag by remember { mutableStateOf(AppLanguage.current()) }
    val system = stringResource(R.string.language_system)
    ChoiceRow(
        stringResource(R.string.language_title),
        AppLanguage.tags.map { it to if (it.isEmpty()) system else AppLanguage.nativeName(it) },
        tag,
        { v ->
            tag = v
            AppLanguage.set(v)
            context.container.onDataChanged() // widgets re-render their texts in the new language
        },
    )
}

@Composable
internal fun CurrencySetting(x: PrefsCtx) {
    var open by rememberSaveable { mutableStateOf(false) }
    val code = x.p.currencyCode.ifEmpty { Formatters.DEFAULT_CURRENCY }
    NavRow(stringResource(R.string.currency_title), code, { open = true })
    if (open) {
        CurrencyPickerSheet(
            code, { open = false }, { v -> x.set { it.copy(currencyCode = v) }; open = false },
            note = stringResource(R.string.currency_change_note),
        )
    }
}

@Composable
internal fun BudgetSetting(x: PrefsCtx) {
    var open by rememberSaveable { mutableStateOf(false) }
    val budget = x.p.monthlyBudget
    SettingsRow(
        Icons.Rounded.Savings, stringResource(R.string.budget_title), { open = true },
        if (budget > 0) stringResource(R.string.budget_per_month, money(budget, ignorePrivacy = true)) else stringResource(R.string.budget_not_set),
        tint = Color(ItemPalette[5]),
    )
    if (open) {
        AmountSheet(
            stringResource(R.string.budget_title), onDismiss = { open = false }, confirmLabel = stringResource(R.string.action_save),
            initialCents = budget,
            extra = {
                Text(stringResource(R.string.budget_hint), style = Penny.type.caption, color = Penny.colors.textSecondary)
                if (budget > 0) {
                    TextButton(onClick = { x.set { it.copy(monthlyBudget = 0) }; open = false }) {
                        Text(stringResource(R.string.budget_remove), color = Penny.colors.danger)
                    }
                }
            },
        ) { cents -> x.set { it.copy(monthlyBudget = cents) }; open = false }
    }
}

@Composable
internal fun MonthlySummarySetting(x: PrefsCtx) {
    val snack = LocalSnack.current
    val deniedText = stringResource(R.string.summary_permission_denied)
    // Without the permission it would silently never arrive: switch it back off and say why.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        x.set { it.copy(notificationPermissionAsked = true, monthlySummary = it.monthlySummary && granted) }
        if (!granted) snack.show(deniedText)
    }
    SwitchRow(
        stringResource(R.string.summary_setting), x.p.monthlySummary,
        { on ->
            // From now on: never a summary for a month that ended before it was switched on.
            val ended = MoneyCalculator.periodOf(LocalDate.now(), x.p.monthStartDay).label.minusMonths(1).toString()
            x.set { it.copy(monthlySummary = on, lastSummaryMonth = if (on) ended else it.lastSummaryMonth) }
            if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        subtitle = stringResource(R.string.summary_setting_sub),
    )
}

@Composable
internal fun MonthStartSetting(x: PrefsCtx) {
    val day = x.p.monthStartDay
    StepperRow(
        stringResource(R.string.pers_month_start), day, 1..28, { v -> x.set { it.copy(monthStartDay = v) } },
        subtitle = if (day == 1) stringResource(R.string.pers_month_start_hint) else stringResource(R.string.pers_month_runs, day, day - 1),
    )
}

@Composable
internal fun ReminderTimeSetting(x: PrefsCtx) {
    var picking by rememberSaveable { mutableStateOf(false) }
    NavRow(
        stringResource(R.string.pers_reminder_time),
        LocalTime.of(x.p.reminderHour, x.p.reminderMinute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(currentLocale())),
        { picking = true },
        subtitle = stringResource(R.string.pers_reminder_time_hint),
    )
    if (picking) {
        TimeDialog(x.p.reminderHour, x.p.reminderMinute, onDismiss = { picking = false }) { h, m ->
            x.set { it.copy(reminderHour = h, reminderMinute = m) }
            picking = false
        }
    }
}

@Composable
internal fun AppLockSetting(x: PrefsCtx) {
    val context = LocalContext.current
    val available = remember(context) { canLock(context) }
    val on = x.p.biometricLock
    SwitchRow(
        stringResource(R.string.pers_biometric), on, { v -> x.set { it.copy(biometricLock = v) } },
        subtitle = stringResource(if (available || on) R.string.pers_biometric_hint else R.string.pers_biometric_unavailable),
        enabled = available || on,
    )
    if (on) {
        ChoiceRow(
            stringResource(R.string.pers_lock_after),
            listOf(
                LockAfter.IMMEDIATELY to stringResource(R.string.pers_lock_immediately),
                LockAfter.ONE_MIN to pluralStringResource(R.plurals.pers_minutes, 1, 1),
                LockAfter.FIVE_MIN to pluralStringResource(R.plurals.pers_minutes, 5, 5),
            ),
            x.p.lockAfter, { v -> x.set { it.copy(lockAfter = v) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val c = Penny.colors
    val context = LocalContext.current
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2,
        title = { Text(stringResource(R.string.pers_reminder_time), style = Penny.type.title, color = c.textPrimary) },
        text = {
            TimePicker(
                state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.surface1, selectorColor = c.accent, containerColor = c.surface2,
                    clockDialSelectedContentColor = c.onAccent,
                    timeSelectorSelectedContainerColor = c.accent.copy(alpha = 0.18f),
                    timeSelectorSelectedContentColor = c.accent,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.pers_set), color = c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = c.textSecondary) } },
    )
}
