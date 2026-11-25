package com.penny.wallet.domain.csv

import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.domain.d
import com.penny.wallet.domain.t
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CsvImportTest {
    private fun import(text: String, digits: Int = 2): Pair<CsvMapping, CsvPreview> {
        val table = CsvReader.read(text)
        val m = CsvImport.guessMapping(table)
        return m to CsvImport.preview(table, m, digits)
    }

    @Test fun pennyOwnExport() {
        val (m, p) = import(
            "date,type,amount_eur,account,to_account,category,goal,note\r\n" +
                "2026-11-01,INCOME,2200.00,Bank,,,,Salary\r\n" +
                "2026-11-02,EXPENSE,12.50,Bank,,Groceries,,\"Milk, bread\"\r\n" +
                "2026-11-03,TRANSFER,100.00,Bank,Cash,,,\r\n",
        )
        assertTrue(m.pennyExport)
        assertEquals(listOf(CsvRow(2, d("2026-11-01"), 2200_00, true, null, "Salary"), CsvRow(3, d("2026-11-02"), 12_50, false, "Groceries", "Milk, bread")), p.rows)
        assertEquals(listOf(CsvRowError(4, CsvRowError.Reason.NOT_EXPENSE_OR_INCOME)), p.errors)
    }

    @Test fun monefyStyleNegativeExpenses() {
        val (_, p) = import(
            "date,account,category,amount,currency,converted amount,currency,description\n" +
                "24/11/2026,Cash,Food,-12.30,EUR,-12.30,EUR,Lunch\n" +
                "25/11/2026,Cash,Salary,1500,EUR,1500,EUR,\n",
        )
        assertEquals(listOf(false to 12_30L, true to 1500_00L), p.rows.map { it.income to it.amountCents })
        assertEquals("Food", p.rows[0].category)
        assertEquals("Lunch", p.rows[0].note)
    }

    @Test fun walletStyleSemicolonsTimesAndTypeWords() {
        val (_, p) = import(
            "account;category;currency;amount;ref_currency_amount;type;payment_type;note;date\n" +
                "Bank;Groceries;EUR;-45,10;-45,10;Expenses;CARD;Supermarket;2026-11-20 18:22:00\n" +
                "Bank;Refunds;EUR;10,00;10,00;Income;CARD;;2026-11-21 09:00:00\n",
        )
        assertEquals(listOf(CsvRow(2, d("2026-11-20"), 45_10, false, "Groceries", "Supermarket"), CsvRow(3, d("2026-11-21"), 10_00, true, "Refunds", null)), p.rows)
    }

    @Test fun moneyManagerStylePositiveAmountsWithTypeColumn() {
        val (_, p) = import(
            "Date,Account,Category,Subcategory,Note,EUR,Income/Expense,Description,Amount,Currency\n" +
                "11/24/2026,Card,Transport,,Train,7.40,Exp.,,7.40,EUR\n" +
                "11/25/2026,Card,Salary,,,2000,Income,,2000,EUR\n",
        )
        assertEquals(listOf(false to 7_40L, true to 2000_00L), p.rows.map { it.income to it.amountCents })
        assertEquals(d("2026-11-24"), p.rows[0].date)
    }

    @Test fun germanBankExport() {
        val (m, p) = import(
            "Buchungstag;Valutadatum;Buchungstext;Verwendungszweck;Betrag;Waehrung\n" +
                "24.11.26;24.11.26;Lastschrift;REWE Markt;-23,45;EUR\n" +
                "25.11.26;25.11.26;Gutschrift;Gehalt November;1.850,00;EUR\n",
        )
        assertEquals("dd.MM.yy", m.datePattern)
        assertEquals(listOf(CsvRow(2, d("2026-11-24"), 23_45, false, null, "REWE Markt"), CsvRow(3, d("2026-11-25"), 1850_00, true, null, "Gehalt November")), p.rows)
    }

    @Test fun debitAndCreditColumns() {
        val (_, p) = import("Date,Description,Debit,Credit\n2026-11-01,Rent,750.00,\n2026-11-02,Refund,,20.00\n2026-11-03,Nothing,,\n")
        assertEquals(listOf(false to 750_00L, true to 20_00L), p.rows.map { it.income to it.amountCents })
        assertEquals(listOf(CsvRowError(4, CsvRowError.Reason.AMOUNT)), p.errors)
    }

    @Test fun noHeaderIsGuessedFromTheData() {
        val (m, p) = import("2026-11-01;Coffee;-3,20\n2026-11-02;Book;-15,00\n")
        assertEquals(false, m.hasHeader)
        assertEquals(listOf(3_20L, 15_00L), p.rows.map { it.amountCents })
        assertEquals(listOf("Coffee", "Book"), p.rows.map { it.note })
    }

    @Test fun badRowsAreReportedNotImported() {
        val (_, p) = import("date,amount,note\n2026-11-01,abc,x\nnot a date,5,y\n2026-11-02,0,z\n2026-11-03,-4,ok\n")
        assertEquals(listOf(5), p.rows.map { it.line })
        assertEquals(listOf(CsvRowError.Reason.AMOUNT, CsvRowError.Reason.DATE, CsvRowError.Reason.ZERO), p.errors.map { it.reason })
    }

    @Test fun duplicatesMatchDateKindAmountAndNote() {
        val rows = listOf(
            CsvRow(2, d("2026-11-02"), 12_50, false, null, "Milk"),
            CsvRow(3, d("2026-11-02"), 12_50, false, null, "Other shop"),
            CsvRow(4, d("2026-11-02"), 12_50, true, null, "Milk"),
            CsvRow(5, d("2026-11-03"), 9_99, false, null, null),
        )
        val existing = listOf(
            t(TxType.EXPENSE, 12_50, "bank", date = "2026-11-02").copy(note = " milk "),
            t(TxType.EXPENSE, 9_99, "bank", date = "2026-11-03"),
            t(TxType.EXPENSE, 9_99, "cash", date = "2026-11-02"),
        )
        assertEquals(setOf(2, 5), CsvImport.duplicates(rows, existing, "bank"))
        // One existing coffee covers one line, not two.
        val coffees = listOf(CsvRow(2, d("2026-11-20"), 3_50, false, null, null), CsvRow(3, d("2026-11-20"), 3_50, false, null, null))
        assertEquals(setOf(2), CsvImport.duplicates(coffees, listOf(t(TxType.EXPENSE, 3_50, "bank", date = "2026-11-20")), "bank"))
    }

    @Test fun fuzzNeverCrashes() {
        val rnd = Random(11)
        val alphabet = "0123456789-+.,;\t\"\n\r()€abcxyz /:'"
        val base = "Date,Description,Amount\n2026-11-01,Coffee,-3.20\n24.11.2026,\"Book, used\",15\n"
        repeat(3_000) { n ->
            val text = if (n % 2 == 0) {
                String(CharArray(rnd.nextInt(0, 200)) { alphabet[rnd.nextInt(alphabet.length)] })
            } else {
                val chars = base.toCharArray()
                repeat(1 + rnd.nextInt(8)) { chars[rnd.nextInt(chars.size)] = alphabet[rnd.nextInt(alphabet.length)] }
                String(chars)
            }
            val table = CsvReader.read(text)
            val m = CsvImport.guessMapping(table)
            val p = CsvImport.preview(table, m, listOf(0, 2, 3)[n % 3])
            p.rows.forEach { assertTrue(it.amountCents > 0) }
            assertTrue(p.rows.size + p.errors.size <= table.size)
        }
    }

    @Test fun pennyExportRoundTripsInEveryCurrency() {
        for ((code, digits, minor) in listOf(Triple("EUR", 2, 1234_56L), Triple("JPY", 0, 123456L), Triple("KWD", 3, 1234_567L))) {
            val b = com.penny.wallet.data.backup.Backup(
                1, "", accounts = listOf(com.penny.wallet.data.db.entities.Account("a", "Bank", "bank", sortOrder = 0, createdAt = 0)),
                categories = listOf(com.penny.wallet.data.db.entities.Category("c", "Food", "other", 0, sortOrder = 0)), goals = emptyList(),
                transactions = listOf(t(TxType.EXPENSE, minor, "a", date = "2026-11-02", cat = "c").copy(note = "Dinner, two")),
                recurring = emptyList(), settings = com.penny.wallet.data.prefs.UserPrefs(currencyCode = code),
            )
            val (_, p) = import(com.penny.wallet.data.backup.Csv.transactions(b), digits)
            assertEquals(code, listOf(CsvRow(2, d("2026-11-02"), minor, false, "Food", "Dinner, two")), p.rows)
        }
    }

    @Test fun bankTypeCodesFallBackToTheSign() {
        val (_, p) = import(
            "Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance\n" +
                "CARD_PAYMENT,Current,2026-11-20 10:00:00,2026-11-20 11:00:00,Coffee,-3.50,0.00,EUR,COMPLETED,96.50\n" +
                "TOPUP,Current,2026-11-21 09:00:00,2026-11-21 09:00:00,Top-up,100.00,0.00,EUR,COMPLETED,196.50\n",
        )
        assertEquals(listOf(false to 3_50L, true to 100_00L), p.rows.map { it.income to it.amountCents })
        assertTrue(p.errors.isEmpty())
    }

    @Test fun dkbStyleHeaderPicksBetragNotWertstellungOrSaldo() {
        val (m, p) = import(
            "Buchungsdatum;Wertstellung;Status;Zahlungspflichtige*r;Zahlungsempfänger*in;Verwendungszweck;Umsatztyp;Saldo (€);Betrag (€)\n" +
                "20.11.26;20.11.26;Gebucht;Ich;REWE;Einkauf;Ausgang;976,55;-23,45\n" +
                "21.11.26;21.11.26;Gebucht;Firma;Ich;Gehalt;Eingang;2.826,55;1.850,00\n",
        )
        assertEquals(CsvColumn.AMOUNT, m.columns[8])
        assertEquals(listOf(false to 23_45L, true to 1850_00L), p.rows.map { it.income to it.amountCents })
    }

    @Test fun pennyExportRowsUseTheirOwnCurrencyDecimals() {
        val text = "date,type,amount,currency,account,to_account,category,goal,note\n2026-11-02,EXPENSE,12.345,KWD,Bank,,Food,,\n2026-11-03,EXPENSE,1234,JPY,Bank,,Food,,\n"
        val (_, p) = import(text, digits = 2)
        assertEquals(listOf(12_35L, 1234_00L), p.rows.map { it.amountCents })
    }

    @Test fun errorsNameTheLineAsInATextEditor() {
        val text = "date,amount,note\n\n2026-11-01,-4,\"two\nlines\"\n2026-11-02,abc,x\n"
        val (table, lines) = CsvReader.readWithLines(text)
        val p = CsvImport.preview(table, CsvImport.guessMapping(table), 2, lines)
        assertEquals(listOf(5), p.errors.map { it.line })
        assertEquals(listOf(3), p.rows.map { it.line })
    }
}
