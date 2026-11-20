package com.penny.wallet.domain.receipt

import org.junit.Assert.assertTrue
import org.junit.Test

/** Realistic receipts, English and German speaking shops, plus the special cases. */
class ReceiptParserTextTest {

    @Test fun ukCafeWithVat() = assertReceipt(
        receipt(
            """
            The Daily Grind
            12 High Street, Leeds LS1 4AB
            VAT Reg No. GB 123 4567 89
            20/11/2026 08:42   Till 2
            Flat White              3.20
            Croissant               2.75
            Bacon Roll              4.50
            Subtotal               10.45
            TOTAL                 £10.45
            VAT @ 20%   Net 8.71   VAT 1.74
            Card Payment           10.45
            Contactless VISA ****1234
            Thank you for visiting!
            """,
        ),
        1045, confident = true,
    )

    @Test fun usRestaurantTipAndFinalTotal() = assertReceipt(
        receipt(
            """
            JOE'S DINER
            Server: Mike   Table 12   Guests 2
            Cheeseburger           12.99
            Caesar Salad            9.50
            Iced Tea  2 @ 2.99      5.98
            Subtotal               28.47
            Sales Tax 8.875%        2.53
            Amount                 31.00
            Tip                     6.20
            Total                  37.20
            VISA XXXXXXXXXXXX1234
            """,
        ),
        3720, confident = true,
    )

    @Test fun usRestaurantSuggestedGratuityTable() = assertReceipt(
        receipt(
            """
            OLIVE & VINE
            Subtotal               28.47
            Tax                     2.53
            Total                  31.00
            Suggested Gratuity:
            18% = $5.12  (Total $36.12)
            20% = $5.69  (Total $36.69)
            22% = $6.26  (Total $37.26)
            Thank you!
            """,
        ),
        3100, confident = true,
    )

    @Test fun ukSupermarketWithTotalSavings() = assertReceipt(
        receipt(
            """
            TESCO Superstore
            MILK SEMI SKIMMED 2.272L    £1.65
            WHOLEMEAL BREAD 800G        £1.40
            CHEDDAR MATURE 400G         £3.50
            BANANAS LOOSE
            0.996 kg @ £0.70/kg         £0.70
            Clubcard Price Saving      -£1.25
            Clubcard Price Saving      -£0.75
            SUBTOTAL                    £7.25
            Total savings               £2.00
            TOTAL                       £5.25
            CARD                        £5.25
            CHANGE DUE                  £0.00
            """,
        ),
        525, confident = true,
    )

    @Test fun germanSupermarketCashWithTaxTable() = assertReceipt(
        receipt(
            """
            REWE Markt GmbH
            Hauptstr. 5, 10115 Berlin
            UID Nr.: DE812706034
                                   EUR
            BIO BANANEN            1,99 A
            VOLLMILCH 3,5%         1,19 A
            TOMATEN
            0,456 kg x 2,99 EUR/kg
                                   1,36 A
            BROT                   2,49 A
            PFAND                  0,25 B
            MINERALWASSER
              2 x 0,89             1,78 B
            --------------------------------
            SUMME EUR              9,06
            ================================
            Geg. BAR EUR          20,00
            Rückgeld BAR EUR      10,94
            Steuer  %    Netto    Steuer   Brutto
            A= 7,0%      6,57     0,46     7,03
            B= 19,0%     1,71     0,32     2,03
            Gesamtbetrag 8,28     0,78     9,06
            20.11.2026 14:32  Bon-Nr.: 4711
            """,
        ),
        906, confident = true,
    )

    @Test fun cardOnlyTerminalSlip() = assertReceipt(
        receipt(
            """
            Café Lindner
            Cappuccino             3,20
            Croissant              2,30
            VISA                   5,50
            --- Kundenbeleg ---
            Kontaktlos
            Terminal-ID: 12345678
            TA-Nr.: 001234   BNr: 0567
            Nr. ############1234
            Datum: 20.11.2026  Uhrzeit: 09:15
            Betrag EUR             5,50
            Zahlung erfolgt
            """,
        ),
        550, confident = true,
    )

    @Test fun discounterWithRabattAndSieSparen() = assertReceipt(
        receipt(
            """
            LIDL Dienstleistung
            Milch 1,5% Fett        0,99 A
            Butter                 2,29 A
            Kaffee Crema 1kg       5,99 A
            Lidl Plus Rabatt      -1,00
            zu zahlen              8,27
            Bar                   10,00
            Rückgeld               1,73
            MWST%   MWST  + Netto  = Brutto
            A  7%   0,54    7,73     8,27
            Summe   0,54    7,73     8,27
            Sie sparen heute       1,00 EUR
            """,
        ),
        827, confident = true,
    )

    @Test fun positiveRabattLineIsNotTheTotal() = assertReceipt(
        receipt(
            """
            EDEKA Center
            Kaffee Crema 1kg      12,99 A
            Rabatt 20%             2,60
            Milch                  1,19 A
            SUMME                 11,58
            EC-Karte              11,58
            """,
        ),
        1158, confident = true,
    )

    @Test fun cashGivenIsTheLargestNumber() = assertReceipt(
        receipt(
            """
            Bäckerei Müller
            Brötchen     6 x 0,45     2,70
            Bienenstich               3,20
            Torte Schwarzwald        14,00
            Kaffee groß               3,50
            Summe                    23,40
            Gegeben                  50,00
            Zurück                   26,60
            """,
        ),
        2340, confident = true,
    )

    @Test fun swissShopWithApostropheThousands() = assertReceipt(
        receipt(
            """
            Elektro Huber AG
            Kaffeemaschine        1'199.00
            Entkalker                15.50
            Total CHF             1'214.50
            MWST 8.1% von 1'214.50   91.00
            Bar CHF               1'300.00
            Rückgeld                 85.50
            """,
        ),
        121_450, confident = true,
    )

    @Test fun noLabelFallsBackToLargestAmount() {
        val r = receipt(
            """
            Kiosk am Bahnhof
            Zeitung              2,80
            Wasser 0,5l          1,50
            Kaugummi             1,20
                                 5,50
            Danke und bis bald!
            """,
        )
        assertReceipt(r, 550, confident = false)
        assertTrue(r.candidates.containsAll(listOf(280L, 150L, 120L)))
    }

    @Test fun noLabelFallbackIgnoresCashAndChange() = assertReceipt(
        receipt(
            """
            Kiosk
            Zeitung              2,80
            Wasser               2,70
                                 5,50
            Bar                 10,00
            Zurück               4,50
            """,
        ),
        550, confident = false,
    )

    @Test fun labelAndAmountOnSeparateLines() = assertReceipt(
        receipt(
            """
            Espresso               2,10
            Tramezzino             4,50
            TOTALE
            EUR 6,60
            Contanti
            10,00
            """,
        ),
        660, confident = true,
    )
}
