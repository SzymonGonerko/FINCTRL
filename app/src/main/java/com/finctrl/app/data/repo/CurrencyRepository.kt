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
            val conn = URL(RATES_URL).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = TIMEOUT_MS
                conn.readTimeout = TIMEOUT_MS
                if (conn.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Serwer kursów zwrócił kod ${conn.responseCode}")
                val rates = JSONObject(conn.inputStream.bufferedReader().readText()).getJSONObject("rates")
                rates.keys().asSequence().associateWith { rates.getDouble(it) }.toSortedMap().also { cache = it }
            } finally {
                conn.disconnect()
            }
        }
    }

    private companion object {
        const val RATES_URL = "https://api.frankfurter.app/latest?from=PLN"
        const val TIMEOUT_MS = 8_000
    }
}
