package com.finctrl.app.data.repo

import com.finctrl.app.data.local.TransactionWithCategory
import com.finctrl.app.data.local.TxType
import java.time.LocalDate

object CsvExporter {
    private const val HEADER = "data;typ;kategoria;kwota;waluta;notatka"

    fun toCsv(items: List<TransactionWithCategory>): String = buildString {
        append(HEADER).append('\n')
        items.forEach { i ->
            val t = i.transaction
            val amount = "%d.%02d".format(t.amount / 100, t.amount % 100)
            append(
                listOf(
                    LocalDate.ofEpochDay(t.date).toString(),
                    if (t.type == TxType.INCOME) "przychod" else "wydatek",
                    i.categoryName ?: "",
                    amount, t.currency, t.note,
                ).joinToString(";") { escape(it) }
            ).append('\n')
        }
    }

    private fun escape(v: String): String {
        val safe = if (v.isNotEmpty() && v[0] in "=+-@") "'$v" else v
        return if (safe.any { it == ';' || it == '"' || it == '\n' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
