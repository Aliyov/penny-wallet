package com.penny.wallet.feature.settings

import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import androidx.compose.foundation.lazy.LazyListScope
import com.penny.wallet.core.ui.components.StepperRow
import com.penny.wallet.core.ui.components.SwitchRow

internal fun LazyListScope.remindersSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    settingsCard("reminders", R.string.pers_section_reminders) {
        val res = LocalResources.current
        val sameDay = stringResource(R.string.pers_same_day)
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
