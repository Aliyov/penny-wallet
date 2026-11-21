package com.penny.wallet.domain.receipt

import java.text.Normalizer

/** What a receipt row's label says about the amount next to it. */
internal enum class RowKind(val weight: Int) {
    /** "Amount due / zu zahlen / à payer": the strongest signal for the final total. */
    TOTAL_STRONG(100),
    /** "Total / Summe / Totale / Toplam". */
    TOTAL(90),
    /** A "total" word with an OCR slip, e.g. "TUTALE" (one letter off). */
    TOTAL_FUZZY(70),
    /** "Betrag / Importe / Montant": an amount label, usually the total on short receipts. */
    AMOUNT(60),
    /** Card payment line: normally equals the total. */
    CARD(45),
    /** No label we know. */
    PLAIN(0),
    /** Subtotal, tax, cash given, change, tip, discount or savings: never the total. */
    EXCLUDED(-1),
}

/**
 * Multilingual receipt vocabulary (EN, DE, FR, ES, IT, PT, NL, PL, TR, AZ, CS/SK, HU, HR/SR/SL,
 * Nordic, Baltic, RU, EL). Matching runs on [normalize]d text: lower case, accents stripped,
 * whole words only (`total` does not match `subtotal`; the subtotal list catches that).
 */
internal object ReceiptKeywords {
    private val strong = listOf(
        "grand total", "total due", "amount due", "balance due", "total to pay", "to pay", "amount to pay", "total payable",
        "total ttc", "net a payer", "a payer", "montant du", "total a payer",
        "zu zahlen", "zahlbetrag", "endbetrag", "endsumme", "gesamtsumme", "gesamtbetrag", "rechnungsbetrag", "summe eur",
        "total a pagar", "a pagar", "importe total", "total importe", "valor a pagar", "total pagar",
        "totale complessivo", "totale da pagare", "da pagare", "totale euro", "totale eur",
        "te betalen", "totaal te betalen",
        "do zaplaty", "suma do zaplaty", "razem do zaplaty", "suma pln",
        "genel toplam", "odenecek tutar", "odenecek", "toplam tutar", "odenilecek", "yekun mebleg", "umumi mebleg",
        "celkem k uhrade", "k uhrade", "k uhrade celkem", "celkova suma", "spolu k uhrade",
        "osszesen fizetendo", "fizetendo", "ukupno za platiti", "za platiti", "za uplatu", "za placilo",
        "att betala", "a betale", "at betale", "yhteensa", "maksettava", "kopa apmaksai", "moketi", "tasuda",
        "итого к оплате", "к оплате", "всего к оплате", "συνολο πληρωμης", "πληρωτεο",
    )
    private val total = listOf(
        "total", "totale", "totaal", "totals", "summe", "gesamt", "suma", "razem", "toplam", "toplami", "summa", "totalt",
        "i alt", "ialt", "celkem", "spolu", "osszesen", "ukupno", "skupaj", "kopa", "is viso", "kokku", "yekun", "cemi",
        "итого", "всего", "συνολο",
    )
    private val amount = listOf(
        "betrag", "montant", "importe", "importo", "bedrag", "kwota", "tutar", "mebleg", "castka", "osszeg", "iznos",
        "brutto", "valor", "amount", "сумма", "ποσο",
    )
    private val card = listOf(
        "visa", "mastercard", "maestro", "amex", "american express", "card", "credit card", "debit", "karte", "kartenzahlung",
        "ec", "ec karte", "girocard", "carte", "cb", "carte bancaire", "tarjeta", "carta", "cartao", "pin", "pinnen",
        "kaart", "karta", "kart", "kredi karti", "bank karti", "contactless", "kontaktlos", "sans contact", "paid",
        "bezahlt", "paye", "pagado", "pagato", "betaald", "zaplacono", "odendi", "apple pay", "google pay", "карта",
    )
    private val subtotal = listOf(
        "subtotal", "sub total", "zwischensumme", "sous total", "subtotale", "subtotaal", "ara toplam", "ara cemi",
        "mezisoucet", "medzisucet", "reszosszeg", "delsumma", "valisumma", "medjuzbir", "промежуточный итог",
    )
    private val tax = listOf(
        "vat", "tax", "taxes", "sales tax", "mwst", "ust", "mehrwertsteuer", "steuer", "tva", "iva", "btw", "kdv", "ptu",
        "vsk", "dph", "afa", "pdv", "ddv", "moms", "alv", "pvm", "edv", "topkdv", "vergi", "ндс", "φπα", "netto", "net",
        "nettobetrag", "ht", "hors taxe", "hors taxes", "base imponible", "imponibile", "base", "matrah",
    )
    /** Words that turn a tax mention into "tax included", e.g. "Summe inkl. MwSt", "Total TTC". */
    private val taxIncluded = listOf(
        "incl", "inkl", "including", "included", "inclusief", "inkludiert", "inclusive", "inklusive", "ttc", "incluido", "incluida", "incluso", "inclusa",
        "compris", "inbegrepen", "dahil", "wliczony", "vcetne", "включая",
    )
    private val excluded = listOf(
        // change
        "change", "ruckgeld", "wechselgeld", "zuruck", "restgeld", "rendu", "monnaie", "rendu monnaie", "cambio", "vuelto",
        "vuelta", "resto", "troco", "wisselgeld", "terug", "reszta", "para ustu", "kusur", "vraceno", "vratit", "visszajaro",
        "kusur", "qaliq", "vaxel", "sdacha", "сдача",
        // cash given
        "cash", "bar", "bargeld", "gegeben", "given", "tendered", "especes", "efectivo", "entregado", "contanti", "contante",
        "dinheiro", "contant", "gotowka", "nakit", "hotovost", "hotovosti", "keszpenz", "gotovina", "kontanter", "kontant",
        "kateinen", "nagd", "received", "recibido", "ricevuto", "наличные",
        // tip
        "tip", "tips", "trinkgeld", "pourboire", "propina", "mancia", "gorjeta", "fooi", "napiwek", "bahsis", "gratuity",
        // discount / savings
        "discount", "discounts", "rabatt", "rabatte", "remise", "remises", "reduction", "descuento", "descuentos", "sconto", "sconti",
        "desconto", "descontos", "korting", "kortingen", "rabat", "rabaty", "indirim", "indirimler",
        "sleva", "kedvezmeny", "popust", "coupon", "gutschein", "voucher", "ahorro", "risparmio", "ersparnis", "sie sparen",
        "you saved", "saved", "saving", "savings", "uw voordeel", "voordeel", "suggested", "tip guide", "economie", "economies", "besparing", "oszczednosc", "endirim", "скидка",
    )

    private val strongRx = compile(strong)
    private val totalRx = compile(total)
    private val amountRx = compile(amount)
    private val cardRx = compile(card)
    private val subtotalRx = compile(subtotal)
    private val taxRx = compile(tax)
    private val taxInclRx = compile(taxIncluded)
    private val excludedRx = compile(excluded)

    private fun compile(words: List<String>): Regex {
        val body = words.map { normalize(it).trim() }.distinct().sortedByDescending { it.length }
            .joinToString("|") { w -> w.split(' ').filter { it.isNotEmpty() }.joinToString("""\s*""") { Regex.escape(it) } }
        return Regex("""(?<![\p{L}])(?:$body)(?![\p{L}])""")
    }

    /** Lower case, accents and punctuation-in-words removed: "À PAYER" → "a payer", "Sous-Total" → "sous total". */
    fun normalize(text: String): String {
        val lower = text.lowercase()
            .replace('ı', 'i').replace('ł', 'l').replace('ø', 'o').replace('ə', 'e').replace("ß", "ss")
            .replace('đ', 'd').replace('æ', 'a')
        val stripped = Normalizer.normalize(lower, Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")
        // "sub-total", "ec-karte", "inkl." → word separators
        return stripped.replace(Regex("""[-_./:*#]+"""), " ")
    }

    /** "18% = $5.12 (Total $36.12)": a tip suggestion or tax-rate row, never the paid total. */
    private val percentFirst = Regex("""^\W*\d{1,2}(?:[.,]\d{1,2})?\s?%""")

    fun classify(text: String): RowKind {
        if (percentFirst.containsMatchIn(text)) return RowKind.EXCLUDED
        val n = normalize(text)
        if (subtotalRx.containsMatchIn(n)) return RowKind.EXCLUDED
        if (excludedRx.containsMatchIn(n)) return RowKind.EXCLUDED
        val isStrong = strongRx.containsMatchIn(n)
        if (!isStrong && taxRx.containsMatchIn(n) && !taxInclRx.containsMatchIn(n)) return RowKind.EXCLUDED
        return when {
            isStrong -> RowKind.TOTAL_STRONG
            totalRx.containsMatchIn(n) -> RowKind.TOTAL
            amountRx.containsMatchIn(n) -> RowKind.AMOUNT
            cardRx.containsMatchIn(n) -> RowKind.CARD
            else -> fuzzyKind(n)
        }
    }

    /** Long label words that survive one OCR slip (two for 8+ letters): "TUTALE" → totale, "HASTERCARD" → mastercard. */
    private val fuzzyTotal = listOf(
        "total", "totale", "totaal", "summe", "gesamt", "razem", "toplam", "toplami", "summa", "totalt", "celkem",
        "osszesen", "ukupno", "skupaj", "kokku", "zahlbetrag", "endbetrag", "endsumme", "gesamtsumme", "gesamtbetrag",
        "rechnungsbetrag", "complessivo", "odenecek", "fizetendo", "yhteensa", "maksettava",
    )
    private val fuzzyCard = listOf("mastercard", "contactless", "kontaktlos", "girocard")
    private val wordLike = Regex("""[\p{L}0-9]+""")

    private fun fuzzyKind(n: String): RowKind {
        // Letter/digit slips inside words: "T0TAL" → "total". Pure numbers are not words.
        val words = wordLike.findAll(n).map { it.value }.filter { w -> w.any { it.isLetter() } }
            .map { it.replace('0', 'o') }.filter { it.length >= 4 }.toList()
        if (words.isEmpty()) return RowKind.PLAIN
        fun near(list: List<String>) = words.any { w -> list.any { k -> closeTo(w, k) } }
        return when {
            near(fuzzyTotal) -> RowKind.TOTAL_FUZZY
            near(fuzzyCard) -> RowKind.CARD
            else -> RowKind.PLAIN
        }
    }

    /** Edit distance ≤ 1 (≤ 2 when the keyword has 8+ letters); short keywords never match fuzzily. */
    internal fun closeTo(word: String, key: String): Boolean {
        if (key.length < 5) return false
        val max = if (key.length >= 8) 2 else 1
        if (kotlin.math.abs(word.length - key.length) > max || word == key) return word == key
        var prev = IntArray(key.length + 1) { it }
        for (i in 1..word.length) {
            val cur = IntArray(key.length + 1)
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..key.length) {
                val cost = if (word[i - 1] == key[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                rowMin = minOf(rowMin, cur[j])
            }
            if (rowMin > max) return false
            prev = cur
        }
        return prev[key.length] <= max
    }
}
