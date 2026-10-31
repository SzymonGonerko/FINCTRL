package com.finctrl.app.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class CurrencyRepository {
    @Volatile private var cache: Map<String, Double>? = null

    suspend fun rates(forceRefresh: Boolean = false): Map<String, Double> {
        cache?.takeIf { !forceRefresh }?.let { return it }
        return withContext(Dispatchers.IO) {
            val conn = URL("https://api.frankfurter.app/latest?from=PLN").openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8_000
                conn.readTimeout = 8_000
                if (conn.responseCode != 200) throw IOException("Serwer kursów zwrócił kod ${conn.responseCode}")
                val rates = JSONObject(conn.inputStream.bufferedReader().readText()).getJSONObject("rates")
                rates.keys().asSequence().associateWith { rates.getDouble(it) }.toSortedMap().also { cache = it }
            } finally {
                conn.disconnect()
            }
        }
    }
}
