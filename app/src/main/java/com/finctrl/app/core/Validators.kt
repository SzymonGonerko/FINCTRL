package com.finctrl.app.core

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object Validators {
    private val MAX_AMOUNT = BigDecimal("1000000000")
    private const val MAX_NAME_LENGTH = 40
    private const val MIN_PASSWORD_LENGTH = 6
    private val PIN_REGEX = Regex("\\d{4,6}")
    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun parseAmount(raw: String): Long {
        val value = raw.trim().replace(',', '.').toBigDecimalOrNull()
            ?: throw ValidationException("Podaj poprawną kwotę.")
        if (value.signum() <= 0) throw ValidationException("Kwota musi być większa od zera.")
        if (value > MAX_AMOUNT) throw ValidationException("Kwota jest zbyt duża.")
        if (value.scale() > 2 && value.stripTrailingZeros().scale() > 2) {
            throw ValidationException("Maksymalnie 2 miejsca po przecinku.")
        }
        return value.setScale(2).movePointRight(2).longValueExact()
    }

    fun requireName(raw: String, field: String = "Nazwa"): String {
        val v = raw.trim()
        if (v.isEmpty()) throw ValidationException("$field nie może być pusta.")
        if (v.length > MAX_NAME_LENGTH) throw ValidationException("$field może mieć maks. $MAX_NAME_LENGTH znaków.")
        return v
    }

    fun validatePin(pin: String): String {
        if (!pin.matches(PIN_REGEX)) throw ValidationException("PIN musi mieć 4–6 cyfr.")
        return pin
    }

    fun validateEmail(raw: String): String {
        val v = raw.trim()
        if (!v.matches(EMAIL_REGEX)) throw ValidationException("Podaj poprawny adres e-mail.")
        return v
    }

    fun validatePassword(raw: String): String {
        if (raw.length < MIN_PASSWORD_LENGTH) throw ValidationException("Hasło musi mieć min. $MIN_PASSWORD_LENGTH znaków.")
        return raw
    }
}

object Money {
    private val POLISH = Locale("pl", "PL")

    fun format(minor: Long, currency: String = "PLN"): String {
        val nf = NumberFormat.getNumberInstance(POLISH).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
        return "${nf.format(minor / 100.0)} $currency"
    }
}
