package com.penny.wallet.domain.receipt

import com.penny.wallet.domain.receipt.RowKind.AMOUNT
import com.penny.wallet.domain.receipt.RowKind.CARD
import com.penny.wallet.domain.receipt.RowKind.EXCLUDED
import com.penny.wallet.domain.receipt.RowKind.PLAIN
import com.penny.wallet.domain.receipt.RowKind.TOTAL
import com.penny.wallet.domain.receipt.RowKind.TOTAL_STRONG
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptKeywordsTest {
    private fun check(kind: RowKind, vararg labels: String) = labels.forEach {
        assertEquals("\"$it\"", kind, ReceiptKeywords.classify(it))
    }

    @Test fun normalize() {
        assertEquals("a payer", ReceiptKeywords.normalize("À PAYER"))
        assertEquals("sous total", ReceiptKeywords.normalize("Sous-Total"))
        assertEquals("strasse", ReceiptKeywords.normalize("Straße"))
        assertEquals("ruckgeld", ReceiptKeywords.normalize("Rückgeld"))
        assertEquals("gotowka", ReceiptKeywords.normalize("Gotówka"))
        assertEquals("celkem k uhrade", ReceiptKeywords.normalize("Celkem k úhradě"))
        assertEquals("para ustu", ReceiptKeywords.normalize("PARA ÜSTÜ"))
        assertEquals("nakit", ReceiptKeywords.normalize("NAKİT"))
        assertEquals("inkl  mwst", ReceiptKeywords.normalize("inkl. MwSt"))
    }

    @Test fun strongTotals() = check(
        TOTAL_STRONG,
        "AMOUNT DUE", "Balance Due", "Grand Total", "TOTAL TTC", "Net à payer", "zu zahlen", "SUMME EUR",
        "Gesamtbetrag", "TOTAL A PAGAR", "Importe total", "TOTALE COMPLESSIVO", "Totale da pagare", "Te betalen",
        "SUMA PLN", "Do zapłaty", "GENEL TOPLAM", "Ödenecek Tutar", "Celkem k úhradě", "Fizetendő", "ИТОГО К ОПЛАТЕ",
    )

    @Test fun plainTotals() = check(
        TOTAL, "TOTAL", "Total:", "Totale", "Totaal", "Summe", "SUMA", "TOPLAM", "Celkem", "Summa",
        "Total incl. VAT", "Summe inkl. MwSt", "TOTAL (IVA incluido)", "ИТОГО",
    )

    @Test fun amountLabels() = check(AMOUNT, "Betrag EUR", "Montant", "Importe", "Importo", "Bedrag", "Kwota", "Brutto")

    @Test fun cardLabels() = check(
        CARD, "VISA", "Mastercard", "EC-Karte", "Kartenzahlung", "girocard", "CB SANS CONTACT", "Tarjeta", "PINNEN",
        "Contactless", "Card Payment", "Apple Pay",
    )

    @Test fun subtotalsExcluded() = check(
        EXCLUDED, "Subtotal", "SUB-TOTAL", "Sub Total", "Zwischensumme", "Sous-total", "Subtotale", "Subtotaal",
        "ARA TOPLAM", "Mezisoučet",
    )

    @Test fun taxExcluded() = check(
        EXCLUDED, "VAT 20%", "Sales Tax", "MwSt 19%", "MWST-Summe", "Netto", "TVA 20%", "Total HT", "IVA 21%",
        "BTW 9%", "PTU A 23,00%", "SUMA PTU", "TOPKDV", "DPH 21 %", "Total VAT", "Total excl. VAT", "Base imponible",
        "davon MwSt", "Enthaltene MwSt",
    )

    @Test fun cashGivenExcluded() = check(
        EXCLUDED, "Cash", "Bar", "Geg. BAR", "Gegeben", "Tendered", "ESPÈCES", "Efectivo", "Contanti", "Dinheiro",
        "Contant", "Gotówka", "NAKİT", "Hotovost",
    )

    @Test fun changeExcluded() = check(
        EXCLUDED, "Change", "Change due", "Rückgeld", "Zurück", "Rendu monnaie", "Cambio", "Resto", "Troco",
        "Wisselgeld", "Reszta", "PARA ÜSTÜ", "Vráceno",
    )

    @Test fun tipsDiscountsSavingsExcluded() = check(
        EXCLUDED, "Tip", "Trinkgeld", "Pourboire", "Propina", "Mancia", "Gratuity", "Suggested Gratuity",
        "Rabatt", "Lidl Plus Rabatt", "Remise", "Descuento", "Sconto", "Korting", "İndirim",
        "Total savings", "Sie sparen", "Sie sparen heute", "You saved", "Clubcard savings", "Gutschein",
    )

    @Test fun singularSavingIsADiscountToo() = check(EXCLUDED, "Clubcard Price Saving")

    @Test fun itemsArePlain() = check(
        PLAIN, "Cappuccino", "Brötchen", "BIO BANANEN", "Croissant", "Flat White", "EAU MINERALE", "Pfand", "",
    )

    @Test fun totalIsWholeWordOnly() {
        assertEquals(EXCLUDED, ReceiptKeywords.classify("SUBTOTAL"))
        assertEquals(PLAIN, ReceiptKeywords.classify("Totalitarian"))
        assertEquals(PLAIN, ReceiptKeywords.classify("Barista blend"))
    }

    @Test fun exclusionBeatsTotal() {
        assertEquals(EXCLUDED, ReceiptKeywords.classify("Total savings"))
        assertEquals(EXCLUDED, ReceiptKeywords.classify("Summe Rabatt"))
        assertEquals(EXCLUDED, ReceiptKeywords.classify("Total tip"))
    }
}
