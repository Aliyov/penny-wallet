package com.penny.wallet.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exact ML Kit output captured on the user's phone (Xiaomi, release 1.3) for Test_Recipe/recipe.jpg,
 * a Carrefour Market receipt from Turin: TOTALE COMPLESSIVO 3,08 · DI CUI IVA 0,33 · MASTERCARD 3,08.
 * The condensed total line came out as "TUTALE OUPLKSO" / "308" and the IVA line was lost.
 */
class RealReceiptRegressionTest {
    private fun l(text: String, l: Int, t: Int, r: Int, b: Int, s: Float) = OcrLine(text, l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat(), s)

    private val carrefour = listOf(
        l("S SPA", 981, 273, 1187, 375, -0.238f),
        l("Cristina, 66", 1054, 277, 1504, 421, -0.196f),
        l("no (T0)", 979, 383, 1263, 508, -0.248f),
        l("UR MARKET", 1033, 93, 1672, 309, -0.234f),
        l("00000000000", 1001, 409, 1428, 560, -0.257f),
        l("000G0II000", 987, 457, 1411, 613, -0.276f),
        l("NIO COMMERCIALLE", 943, 491, 1519, 672, -0.230f),
        l("prestazione", 1132, 556, 1608, 683, -0.154f),
        l("AERO LATTINA M", 556, 746, 1133, 909, -0.180f),
        l("MARGHERITA 26X", 538, 820, 1131, 961, -0.130f),
        l("PROMO", 574, 939, 781, 1029, -0.167f),
        l("SPESAMICA", 561, 1007, 951, 1099, -0.065f),
        l("MASTERCARD", 473, 2008, 866, 2099, -0.062f),
        l("Totale Sconti 0ttenuti", 216, 1175, 1127, 1314, -0.068f),
        l("TUTALE OUPLKSO", 231, 1589, 1005, 1846, -0.086f),
        l("SEPA-FAST", 641, 2170, 975, 2241, 0.000f),
        l("CONTACTLESS", 597, 2245, 1015, 2326, 0.029f),
        l("A0000000041010", 330, 2547, 870, 2657, 0.058f),
        l("HASTERCARD", 343, 2629, 726, 2720, 0.032f),
        l("000000XXXXX*0000", 378, 2703, 949, 2823, 0.078f),
        l("01", 914, 2745, 1075, 2832, 0.077f),
        l("20/11/2026", 346, 2786, 727, 2860, 0.016f),
        l("10000", 1076, 2908, 1270, 2988, 0.090f),
        l("PREZZO", 1437, 623, 1704, 703, -0.072f),
        l("0,49 22,00", 1573, 694, 2062, 786, -0.035f),
        l("4,41 10,00", 1613, 776, 2066, 881, -0.038f),
        l("-0,40", 1543, 889, 1755, 972, -0.028f),
        l("-1,42", 1542, 983, 1759, 1067, -0.042f),
        l("-1,82", 1537, 1156, 1762, 1251, -0.045f),
        l("308", 1503, 1573, 1707, 1779, 0.020f),
        l("CARREFOUR MARKET 0613\\ MTorino\\10125", 333, 2309, 1783, 2455, 0.040f),
        l("100000000000001", 363, 2397, 894, 2498, 0.042f),
        l("00000001", 337, 2474, 660, 2557, 0.019f),
        l("3,08", 1575, 2015, 1748, 2100, 0.000f),
        l("%Iva", 1886, 611, 2062, 676, 0.000f),
    )

    @Test fun carrefourTurinAsReadOnThePhone() {
        val r = ReceiptParser.parse(carrefour)
        assertEquals(308L, r.totalCents)
        assertTrue(r.confident)
        assertEquals(308L, r.candidates.first())
    }

    @Test fun vatRateColumnIsNotOfferedAsAnAmount() {
        val r = ReceiptParser.parse(carrefour)
        assertFalse("22,00 is the IVA rate column", 2200L in r.candidates)
        assertFalse("10,00 is the IVA rate column", 1000L in r.candidates)
    }

    @Test fun withoutTheCardLineTheLostCommaTotalIsStillOfferedFirstButNotConfident() {
        val noCard = carrefour.filterNot { it.text == "3,08" }
        val r = ReceiptParser.parse(noCard)
        assertEquals(308L, r.totalCents)
        assertFalse(r.confident)
    }

    @Test fun withTheTotalLineUnreadableTheCardLineGivesTheTotal() {
        val noTotal = carrefour.filterNot { it.text == "TUTALE OUPLKSO" || it.text == "308" }
        val r = ReceiptParser.parse(noTotal)
        assertEquals(308L, r.totalCents)
        assertTrue(r.confident)
    }

    @Test fun cleanTextOfTheSameReceipt() {
        val r = ReceiptParser.parseText(
            """
            CARREFOUR MARKET
            DESCRIZIONE PREZZO %Iva
            COCA COLA ZERO LATTINA M 0,49 22,00
            PIZZA MARGHERITA 26X 4,41 10,00
            Sconto PROMO -0,40
            Sconto SPESAMICA -1,42
            Totale Sconti Ottenuti -1,82
            TOTALE COMPLESSIVO 3,08
            DI CUI IVA 0,33
            MASTERCARD 3,08
            """.trimIndent(),
        )
        assertEquals(308L, r.totalCents)
        assertTrue(r.confident)
        assertFalse(2200L in r.candidates)
    }
}
