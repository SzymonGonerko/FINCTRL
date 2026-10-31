package com.finctrl.app

import com.finctrl.app.data.local.TransactionEntity
import com.finctrl.app.data.local.TransactionWithCategory
import com.finctrl.app.data.local.TxType
import com.finctrl.app.data.repo.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExporterTest {
    private fun item(note: String, amount: Long = 1234, type: TxType = TxType.EXPENSE) = TransactionWithCategory(
        TransactionEntity(1, amount, type, 1, note, date = 0), "Jedzenie", "#fff",
    )

    @Test fun `exports header and formatted row`() {
        val csv = CsvExporter.toCsv(listOf(item("obiad")))
        assertEquals("data;typ;kategoria;kwota;waluta;notatka\n1970-01-01;wydatek;Jedzenie;12.34;PLN;obiad\n", csv)
    }

    @Test fun `quotes separators and neutralises formulas`() {
        val csv = CsvExporter.toCsv(listOf(item("a;b"), item("=SUM(A1)")))
        val lines = csv.trim().lines()
        assertEquals("1970-01-01;wydatek;Jedzenie;12.34;PLN;\"a;b\"", lines[1])
        assertEquals("1970-01-01;wydatek;Jedzenie;12.34;PLN;'=SUM(A1)", lines[2])
    }
}
