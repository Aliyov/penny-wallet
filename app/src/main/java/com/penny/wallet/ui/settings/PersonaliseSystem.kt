package com.penny.wallet.ui.settings

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
import com.penny.wallet.ui.components.ChoiceRow
import com.penny.wallet.ui.components.NavRow
import com.penny.wallet.ui.components.StepperRow
import com.penny.wallet.ui.components.SwitchRow
import com.penny.wallet.ui.theme.Penny

internal fun LazyListScope.remindersSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("reminders", "Reminders") {
        var picking by rememberSaveable { mutableStateOf(false) }
        NavRow(
            "Reminder time", "%02d:%02d".format(p.reminderHour, p.reminderMinute), { picking = true },
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
            "Default days before", p.defaultRemindDaysBefore, 0..7, { v -> x.set { it.copy(defaultRemindDaysBefore = v) } },
            format = { if (it == 0) "Same day" else "$it d" }, subtitle = "Used for new recurring rules",
            onReset = x.reset(p.defaultRemindDaysBefore, d.defaultRemindDaysBefore) { it.copy(defaultRemindDaysBefore = d.defaultRemindDaysBefore) },
        )
        SwitchRow(
            "Overdue badge", p.overdueBadge, { v -> x.set { it.copy(overdueBadge = v) } },
            onReset = x.reset(p.overdueBadge, d.overdueBadge) { it.copy(overdueBadge = d.overdueBadge) },
        )
        SwitchRow(
            "Quiet mode", p.quietMode, { v -> x.set { it.copy(quietMode = v) } }, subtitle = "No notifications at all",
            onReset = x.reset(p.quietMode, d.quietMode) { it.copy(quietMode = d.quietMode) },
        )
    }
}

internal fun LazyListScope.backupSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("backup", "Backup") {
        ChoiceRow(
            "Frequency", listOf(BackupFrequency.DAILY to "Daily", BackupFrequency.WEEKLY to "Weekly", BackupFrequency.MONTHLY to "Monthly"),
            p.backupFrequency, { v -> x.set { it.copy(backupFrequency = v) } },
            subtitle = "Choose the backup folder on the Backup screen",
            onReset = x.reset(p.backupFrequency, d.backupFrequency) { it.copy(backupFrequency = d.backupFrequency) },
        )
        StepperRow(
            "Files kept", p.backupKeep, 1..30, { v -> x.set { it.copy(backupKeep = v) } },
            subtitle = "Older backups are deleted",
            onReset = x.reset(p.backupKeep, d.backupKeep) { it.copy(backupKeep = d.backupKeep) },
        )
    }
}

internal fun LazyListScope.securitySection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("security", "Security") {
        SwitchRow(
            "Biometric app lock", p.biometricLock, { v -> x.set { it.copy(biometricLock = v) } },
            subtitle = "Fingerprint, face or device PIN",
            onReset = x.reset(p.biometricLock, d.biometricLock) { it.copy(biometricLock = d.biometricLock) },
        )
        if (p.biometricLock) {
            ChoiceRow(
                "Lock after", listOf(LockAfter.IMMEDIATELY to "Immediately", LockAfter.ONE_MIN to "1 min", LockAfter.FIVE_MIN to "5 min"),
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
        title = { Text("Reminder time", style = Penny.type.title, color = c.textPrimary) },
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
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Set", color = c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) } },
    )
}
