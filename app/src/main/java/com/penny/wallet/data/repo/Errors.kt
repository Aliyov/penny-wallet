package com.penny.wallet.data.repo

class InsufficientAvailableException(val availableCents: Long) : IllegalArgumentException("Not enough available")

/** A user-correctable input problem; the feature layer turns [reason] into a localized message. */
class InvalidInputException(val reason: Reason) : IllegalArgumentException(reason.name) {
    enum class Reason { AMOUNT_NOT_POSITIVE, TARGET_NOT_POSITIVE, SAME_ACCOUNTS }
}
