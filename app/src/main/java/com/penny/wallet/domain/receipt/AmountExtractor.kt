package com.penny.wallet.domain.receipt

/**
 * Finds money amounts with exactly two decimals in a line of receipt text, in any common format:
 * `5.50`, `5,50`, `1.234,56`, `1,234.56`, `1'234.50`, `€5.50`, `5,50 EUR`, `5,50 A` (tax class).
 * Ignores dates (`20.11.2026`), percentages (`19,00%`), weights/volumes (`0,45 kg`),
 * quantities (`2,00 x`), unit prices (`2,99 €/kg`) and negative amounts (`-1,20`, `-£0.75`, `1,20-`) such as discounts.
 */
internal object AmountExtractor {
    const val MAX_CENTS = 10_000_000L // €100,000: anything above is a misread number

    /** Digit-ish tokens where OCR read 0 as the letter O, e.g. `5,5O`, `1O,00`. */
    private val oToken = Regex("""(?<![\w.,])\d[\dOo]*[.,] ?[\dOo]{2}(?!\w)""")

    /** OCR split after the separator: `5, 50`. */
    private val splitDecimal = Regex("""(\d)([.,]) (\d{2})(?![\d.,])""")

    private val amount = Regex(
        """(?<![\d.,'])(-(?:\s?[€£$₺]\s?)?)?(\d{1,3}(?:[.,']\d{3})+|\d{1,6})[.,](\d{2})""" +
            """(?![\d%]|[.,]\d|\s?%|\s?(?:kg|g|l|ml|cl|x|st|pcs)\b|\s?(?:[€£$]|eur)?\s?/)(-)?""",
        RegexOption.IGNORE_CASE,
    )

    /** Fixes common OCR slips so [amounts] can read them. */
    fun clean(text: String): String {
        val noO = oToken.replace(text) { m -> m.value.replace('O', '0').replace('o', '0') }
        return splitDecimal.replace(noO) { m -> m.groupValues[1] + m.groupValues[2] + m.groupValues[3] }
    }

    /** Positive amounts in cents, in reading order (left to right). */
    fun amounts(text: String): List<Long> = amount.findAll(clean(text)).mapNotNull { m ->
        val negative = m.groupValues[1].isNotEmpty() || m.groupValues[4].isNotEmpty()
        if (negative) return@mapNotNull null
        val whole = m.groupValues[2].filter { it.isDigit() }.toLongOrNull() ?: return@mapNotNull null
        val cents = whole * 100 + m.groupValues[3].toLong()
        cents.takeIf { it in 1..MAX_CENTS }
    }.toList()
}
