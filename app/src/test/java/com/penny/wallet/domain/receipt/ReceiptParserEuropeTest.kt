package com.penny.wallet.domain.receipt

import org.junit.Test

/** Realistic receipts from French, Spanish, Italian, Dutch, Polish, Turkish, Portuguese and Czech shops. */
class ReceiptParserEuropeTest {

    @Test fun frenchSupermarket() = assertReceipt(
        receipt(
            """
            CARREFOUR MARKET
            SIRET 123 456 789 00012
            BAGUETTE TRADITION      1,10
            CAMEMBERT PRESIDENT     2,45
            VIN ROUGE BORDEAUX      6,90
            EAU MINERALE 6X1,5L     3,20
            -------------------
            TOTAL TTC          13,65 €
            TVA 5,5%   HT 6,40   TVA 0,35
            TVA 20%    HT 5,75   TVA 1,15
            ESPECES            20,00 €
            RENDU MONNAIE       6,35 €
            Merci de votre visite
            """,
        ),
        1365, confident = true,
    )

    @Test fun frenchBrasserieCard() = assertReceipt(
        receipt(
            """
            BRASSERIE DU PORT
            Table 7   Couverts 2
            2 Café crème        2,80    5,60
            1 Croque monsieur           9,50
            Total HT                   13,73
            TVA 10%                     1,37
            Net à payer                15,10
            CB SANS CONTACT            15,10
            """,
        ),
        1510, confident = true,
    )

    @Test fun spanishSupermarketWithTaxTotalRow() = assertReceipt(
        receipt(
            """
            MERCADONA, S.A.
            NIF A-46103834
            FACTURA SIMPLIFICADA: 2345-012-678901
            20/11/2026 18:05
            1 LECHE ENTERA                0,89
            2 PAN DE MOLDE       1,35     2,70
            1 ACEITE OLIVA                7,95
            1 TOMATE FRITO                1,15
            TOTAL (€)                    12,69
            EFECTIVO                     20,00
            CAMBIO                        7,31
            IVA      BASE IMPONIBLE (€)   CUOTA (€)
            4%            3,45               0,14
            10%           8,27               0,83
            TOTAL        11,72               0,97
            """,
        ),
        1269, confident = true,
    )

    @Test fun spanishBarIvaIncluido() = assertReceipt(
        receipt(
            """
            BAR RESTAURANTE EL PATIO
            C/ Mayor 12, Madrid
            Mesa: 4   Camarero: Luis
            2 CAÑA                 2,00    4,00
            1 TORTILLA ESPAÑOLA            6,50
            1 CALAMARES                    9,80
            TOTAL (IVA incluido)          20,30
            Base imponible 18,45   IVA 10% 1,85
            EFECTIVO                      50,00
            CAMBIO                        29,70
            """,
        ),
        2030, confident = true,
    )

    @Test fun italianBar() = assertReceipt(
        receipt(
            """
            BAR PASTICCERIA ROMA
            Via Garibaldi 10 - 20121 Milano
            P.IVA 01234567890
            DOCUMENTO COMMERCIALE
            di vendita o prestazione
            DESCRIZIONE          IVA    Prezzo(€)
            CAPPUCCINO           10%       1,50
            CORNETTO             10%       1,30
            SPREMUTA             10%       3,50
            TOTALE COMPLESSIVO             6,30
            di cui IVA                     0,57
            Pagamento contante            10,00
            Pagamento elettronico          0,00
            Resto                          3,70
            20-11-2026 10:12  DOC.N. 0042-0017
            """,
        ),
        630, confident = true,
    )

    @Test fun dutchSupermarketPinnen() = assertReceipt(
        receipt(
            """
            Albert Heijn 1234
            Kalverstraat 1, Amsterdam
            OMSCHRIJVING            BEDRAG
            HALFV MELK               1,19
            KAAS JONG BLOK           5,49
            APPELS ELSTAR            2,29
            BONUS KAAS              -1,00
            SUBTOTAAL                8,97
            UW VOORDEEL              1,00
            TOTAAL                   7,97
            PINNEN                   7,97
            BTW      OVER      EUR
            9%       7,31      0,66
            TOTAAL   7,31      0,66
            """,
        ),
        797, confident = true,
    )

    @Test fun dutchCafeCash() = assertReceipt(
        receipt(
            """
            Café De Gouden Leeuw
            2 Koffie verkeerd    3,40    6,80
            1 Appeltaart                 4,50
            Totaal                      11,30
            BTW 9%                       0,93
            Contant                     20,00
            Wisselgeld                   8,70
            """,
        ),
        1130, confident = true,
    )

    @Test fun polishParagon() = assertReceipt(
        receipt(
            """
            BIEDRONKA "JERONIMO MARTINS POLSKA" S.A.
            ul. Żniwna 5, 62-025 Kostrzyn
            NIP 779-10-11-327
            2026-11-20                     nr wydr. 123456
            PARAGON FISKALNY
            CHLEB WIEJSKI       1 x 4,99    4,99C
            MLEKO 2% 1L         2 x 3,49    6,98C
            MASŁO EXTRA         1 x 7,99    7,99C
            PIWO ŻYWIEC         1 x 4,29    4,29A
            SPRZEDAŻ OPODATKOWANA A        4,29
            PTU A 23,00%                   0,80
            SPRZEDAŻ OPODATKOWANA C       19,96
            PTU C 5,00%                    0,95
            SUMA PTU                       1,75
            SUMA PLN                      24,25
            Gotówka                       50,00
            Reszta                        25,75
            """,
        ),
        2425, confident = true,
    )

    @Test fun turkishMarket() = assertReceipt(
        receipt(
            """
            MİGROS TİCARET A.Ş.
            ATATÜRK MAH. İSTANBUL
            TARİH: 20.11.2026   SAAT: 19:45
            FİŞ NO: 0042
            EKMEK            %1     *10,00
            SÜT 1L           %1     *32,50
            PEYNİR           %1    *145,90
            DETERJAN         %20    *89,95
            TOPKDV                  *16,86
            TOPLAM                 *278,35
            NAKİT                  *300,00
            PARA ÜSTÜ               *21,65
            """,
        ),
        27_835, confident = true,
    )

    @Test fun portugueseSupermarket() = assertReceipt(
        receipt(
            """
            PINGO DOCE
            Contribuinte: 500829993
            FATURA SIMPLIFICADA FS 012/123456
            PAO DE FORMA            1,49
            IOGURTE NATURAL x4      1,99
            BACALHAU                8,75
            VINHO TINTO             3,99
            TOTAL A PAGAR          16,22
            Taxa  Incidência   IVA
            6%    11,54        0,69
            23%    3,24        0,75
            Dinheiro               20,00
            Troco                   3,78
            """,
        ),
        1622, confident = true,
    )

    @Test fun czechCafe() = assertReceipt(
        receipt(
            """
            Kavárna U Mostu s.r.o.
            IČO: 12345678  DIČ: CZ12345678
            Účtenka č. 2026/1120/015
            Espresso         2 x 55,00     110,00
            Medovník         1 x 89,00      89,00
            Minerálka        1 x 45,00      45,00
            Mezisoučet                     244,00
            DPH 12 %   Základ 217,86   DPH 26,14
            Celkem k úhradě             244,00 Kč
            Hotovost                       300,00
            Vráceno                         56,00
            """,
        ),
        24_400, confident = true,
    )
}
