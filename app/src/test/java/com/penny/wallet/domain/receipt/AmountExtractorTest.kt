package com.penny.wallet.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountExtractorTest {
    private fun amounts(text: String) = AmountExtractor.amounts(text)
    private fun one(expected: Long, text: String) = assertEquals("\"$text\"", listOf(expected), amounts(text))
    private fun none(text: String) = assertEquals("\"$text\" must not be an amount", emptyList<Long>(), amounts(text))

    // ---- formats that are amounts ----

    @Test fun dotDecimal() = one(550, "5.50")
    @Test fun commaDecimal() = one(550, "5,50")
    @Test fun europeanThousands() = one(123_456, "1.234,56")
    @Test fun englishThousands() = one(123_456, "1,234.56")
    @Test fun swissThousands() = one(123_450, "1'234.50")
    @Test fun euroSymbolBefore() = one(550, "€5.50")
    @Test fun euroSymbolAfter() = one(550, "5,50 €")
    @Test fun euroSymbolAfterNoSpace() = one(550, "5,50€")
    @Test fun eurCodeAfter() = one(550, "5.50 EUR")
    @Test fun eurCodeBefore() = one(550, "EUR 5,50")
    @Test fun poundSymbol() = one(1045, "£10.45")
    @Test fun dollarThousands() = one(123_456, "$1,234.56")
    @Test fun taxClassLetter() = one(550, "5,50 A")
    @Test fun taxClassLetterGlued() = one(199, "1,99A")
    @Test fun turkishStarPrefix() = one(27_835, "*278,35")
    @Test fun ocrLetterOInCents() = one(550, "5,5O")
    @Test fun ocrLetterOInWhole() = one(1000, "1O,00")
    @Test fun ocrLowercaseO() = one(1000, "1o,0o")
    @Test fun splitAfterSeparator() = one(550, "5, 50")
    @Test fun labelledLine() = one(1365, "TOTAL TTC          13,65 €")
    @Test fun sixDigitWhole() = one(9_999_999, "99999,99")
    @Test fun upToMax() = one(AmountExtractor.MAX_CENTS, "100.000,00")
    @Test fun leadingZero() = one(5, "0,05")

    @Test fun severalAmountsInReadingOrder() {
        assertEquals(listOf(135L, 270L), amounts("2 PAN DE MOLDE   1,35   2,70"))
        assertEquals(listOf(5500L, 11000L), amounts("Espresso   2 x 55,00     110,00"))
        assertEquals(listOf(170L, 32L, 202L), amounts("B= 19,0%     1,70     0,32     2,02"))
    }

    @Test fun quantityBeforeAmountKeepsAmount() = assertEquals(listOf(89L), amounts("2 x 0,89"))

    // ---- things that are not amounts ----

    @Test fun dateFullYear() = none("20.11.2026")
    @Test fun dateShortYear() = none("20.11.26")
    @Test fun dateSlashes() = none("20/11/2026")
    @Test fun dateIso() = none("2026-11-20")
    @Test fun time() = none("12:30")
    @Test fun dateAndTime() = none("Datum: 20.11.26  Uhrzeit: 12:30")
    @Test fun percentWithDecimals() = none("19,00%")
    @Test fun percentWithSpace() = none("19,00 %")
    @Test fun percentInteger() = none("7%")
    @Test fun weightThreeDecimals() = none("0,456 kg")
    @Test fun weightTwoDecimals() = none("1,50 kg")
    @Test fun weightGlued() = none("1,50kg")
    @Test fun volumeLitres() = none("0,75 l")
    @Test fun quantityFactor() = none("2,00 x")
    @Test fun negativeLeadingMinus() = none("-1,20")
    @Test fun negativeTrailingMinus() = none("1,20-")
    @Test fun negativeWithCurrencySymbol() = none("-£0.75")
    @Test fun integers() = none("Tisch 12  Gäste 2  Bon 4711")
    @Test fun phoneNumber() = none("Tel. +49 30 12345678")
    @Test fun phoneNumberDotted() = none("Tel: 0221-12.34.56")
    @Test fun maskedCardNumber() = none("VISA ************1234")
    @Test fun cardNumberGroups() = none("Karte 4111 1111 1111 1111")
    @Test fun threeDecimals() = none("1,234")
    @Test fun threeDecimalsDot() = none("12.345")
    @Test fun oneDecimal() = none("3,5")
    @Test fun aboveMax() = none("123.456,78")
    @Test fun zero() = none("0,00")
    @Test fun emptyText() = none("")
    @Test fun vatNumber() = none("UID Nr.: DE812706034")

    @Test fun percentAndWeightMixedWithRealAmount() {
        assertEquals(listOf(119L), amounts("VOLLMILCH 3,5%       1,19 A"))
        assertEquals(listOf(136L), amounts("TOMATEN 0,456 kg      1,36 A").takeLast(1))
        assertEquals(listOf(174L), amounts("VAT 20.00%   1.74"))
    }

    @Test fun cleanFixesOcrSlips() {
        assertEquals("5,50", AmountExtractor.clean("5,5O"))
        assertEquals("10,00", AmountExtractor.clean("1O,00"))
        assertEquals("5,50", AmountExtractor.clean("5, 50"))
        assertEquals("BROT 2,49", AmountExtractor.clean("BROT 2,49"))
    }

    @Test fun cleanLeavesWordsAlone() {
        assertEquals("TOTAL COOKIES", AmountExtractor.clean("TOTAL COOKIES"))
    }
}
