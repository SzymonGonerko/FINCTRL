package com.finctrl.app.core

import android.database.sqlite.SQLiteException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class UiMessage(val text: String, val retry: (() -> Unit)? = null)

class ValidationException(message: String) : Exception(message)

class ErrorHandler {
    fun toMessage(error: Throwable): String = when (error) {
        is ValidationException -> error.message ?: "Nieprawidłowe dane."
        is UnknownHostException, is SocketTimeoutException -> "Brak połączenia z siecią. Sprawdź internet i spróbuj ponownie."
        is SQLiteException -> "Błąd bazy danych. Spróbuj ponownie."
        is IOException -> "Błąd odczytu/zapisu lub sieci: ${error.message ?: "nieznany"}."
        else -> error.message?.takeIf { it.isNotBlank() } ?: "Wystąpił nieoczekiwany błąd."
    }
}
