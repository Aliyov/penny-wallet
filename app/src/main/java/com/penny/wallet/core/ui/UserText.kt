package com.penny.wallet.core.ui

import com.penny.wallet.AppContainer
import com.penny.wallet.R
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.data.repo.InsufficientAvailableException
import com.penny.wallet.data.repo.InvalidInputException
import java.util.Locale

/** Money as text for ViewModel messages, in the user's currency and the app locale. */
fun AppContainer.moneyText(cents: Long): String =
    Formatters.money(cents, Formatters.of(settings.prefs.value, Locale.getDefault()))

fun AppContainer.userMessage(e: Throwable): String = when (e) {
    is InsufficientAvailableException -> context.getString(R.string.error_only_available, moneyText(e.availableCents))
    is InvalidInputException -> context.getString(
        when (e.reason) {
            InvalidInputException.Reason.AMOUNT_NOT_POSITIVE -> R.string.error_amount_positive
            InvalidInputException.Reason.TARGET_NOT_POSITIVE -> R.string.error_target_positive
            InvalidInputException.Reason.SAME_ACCOUNTS -> R.string.error_same_accounts
            InvalidInputException.Reason.SPLIT_TOO_FEW -> R.string.error_split_too_few
            InvalidInputException.Reason.SPLIT_NO_CATEGORY -> R.string.error_split_no_category
            InvalidInputException.Reason.NAME_REQUIRED -> R.string.error_name_required
            InvalidInputException.Reason.DEBT_OVERPAID -> R.string.error_debt_overpaid
            InvalidInputException.Reason.DEBT_BELOW_PAID -> R.string.error_debt_below_paid
            InvalidInputException.Reason.DEBT_HAS_PAYMENTS -> R.string.error_debt_has_payments
        },
    )
    // require()/error() in ViewModels and BackupManager already carry a localized message.
    is IllegalArgumentException, is IllegalStateException -> e.message ?: context.getString(R.string.error_generic)
    else -> {
        android.util.Log.w("Penny", "Unexpected error", e)
        context.getString(R.string.error_generic)
    }
}
