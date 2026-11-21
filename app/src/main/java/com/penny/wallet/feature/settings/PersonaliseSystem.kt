package com.penny.wallet.feature.settings

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.penny.wallet.data.prefs.BackupFrequency
import com.penny.wallet.data.prefs.LockAfter
import com.penny.wallet.core.ui.components.ChoiceRow
import com.penny.wallet.core.ui.components.NavRow
import com.penny.wallet.core.ui.components.StepperRow
import com.penny.wallet.core.ui.components.SwitchRow
import com.penny.wallet.core.ui.theme.Penny

internal fun LazyListScope.remindersSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("reminders", R.string.pers_section_reminders) {
        var picking by rememberSaveable { mutableStateOf(false) }
        val res = LocalResources.current
        val sameDay = stringResource(R.string.pers_same_day)
        NavRow(
            stringResource(R.string.pers_reminder_time), "%02d:%02d".format(p.reminderHour, p.reminderMinute), { picking = true },
            onReset = x.reset(p.reminderHour to p.reminderMinute, d.reminderHour to d.reminderMinute) {
                it.copy(reminderHour = d.reminderHour, reminderMinute = d.reminderMinute)
            },
        )
        if (picking) {
            TimeDialog(p.reminderHour, p.reminderMinute, onDismiss = { picking = false }) { h, m ->
                x.set { it.copy(reminderHour = h, reminderMinute = m) }
                picking = false
            }
        }
        StepperRow(
            stringResource(R.string.pers_days_before), p.defaultRemindDaysBefore, 0..7, { v -> x.set { it.copy(defaultRemindDaysBefore = v) } },
            format = { if (it == 0) sameDay else res.getQuantityString(R.plurals.pers_days_count, it, it) },
            subtitle = stringResource(R.string.pers_days_before_hint),
            onReset = x.reset(p.defaultRemindDaysBefore, d.defaultRemindDaysBefore) { it.copy(defaultRemindDaysBefore = d.defaultRemindDaysBefore) },
        )
        SwitchRow(
            stringResource(R.string.pers_overdue_badge), p.overdueBadge, { v -> x.set { it.copy(overdueBadge = v) } },
            onReset = x.reset(p.overdueBadge, d.overdueBadge) { it.copy(overdueBadge = d.overdueBadge) },
        )
        SwitchRow(
            stringResource(R.string.pers_quiet_mode), p.quietMode, { v -> x.set { it.copy(quietMode = v) } },
            subtitle = stringResource(R.string.pers_quiet_mode_hint),
            onReset = x.reset(p.quietMode, d.quietMode) { it.copy(quietMode = d.quietMode) },
        )
    }
}

internal fun LazyListScope.backupSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("backup", R.string.pers_section_backup) {
        ChoiceRow(
            stringResource(R.string.pers_backup_frequency),
            listOf(
                BackupFrequency.DAILY to stringResource(R.string.pers_daily), BackupFrequency.WEEKLY to stringResource(R.string.pers_weekly),
                BackupFrequency.MONTHLY to stringResource(R.string.pers_monthly),
            ),
            p.backupFrequency, { v -> x.set { it.copy(backupFrequency = v) } },
            subtitle = stringResource(R.string.pers_backup_frequency_hint),
            onReset = x.reset(p.backupFrequency, d.backupFrequency) { it.copy(backupFrequency = d.backupFrequency) },
        )
        StepperRow(
            stringResource(R.string.pers_files_kept), p.backupKeep, 1..30, { v -> x.set { it.copy(backupKeep = v) } },
            subtitle = stringResource(R.string.pers_files_kept_hint),
            onReset = x.reset(p.backupKeep, d.backupKeep) { it.copy(backupKeep = d.backupKeep) },
        )
    }
}

internal fun LazyListScope.securitySection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("security", R.string.pers_section_security) {
        SwitchRow(
            stringResource(R.string.pers_biometric), p.biometricLock, { v -> x.set { it.copy(biometricLock = v) } },
            subtitle = stringResource(R.string.pers_biometric_hint),
            onReset = x.reset(p.biometricLock, d.biometricLock) { it.copy(biometricLock = d.biometricLock) },
        )
        if (p.biometricLock) {
            ChoiceRow(
                stringResource(R.string.pers_lock_after),
                listOf(
                    LockAfter.IMMEDIATELY to stringResource(R.string.pers_lock_immediately),
                    LockAfter.ONE_MIN to pluralStringResource(R.plurals.pers_minutes, 1, 1),
                    LockAfter.FIVE_MIN to pluralStringResource(R.plurals.pers_minutes, 5, 5),
                ),
                p.lockAfter, { v -> x.set { it.copy(lockAfter = v) } },
                onReset = x.reset(p.lockAfter, d.lockAfter) { it.copy(lockAfter = d.lockAfter) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val c = Penny.colors
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
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
