package com.finctrl.app.core

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object Validators {
    private val MAX_AMOUNT = BigDecimal("1000000000")

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
        if (v.length > 40) throw ValidationException("$field może mieć maks. 40 znaków.")
        return v
    }

    fun validatePin(pin: String): String {
        if (!pin.matches(Regex("\\d{4,6}"))) throw ValidationException("PIN musi mieć 4–6 cyfr.")
        return pin
    }

    fun validateEmail(raw: String): String {
        val v = raw.trim()
        if (!v.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))) throw ValidationException("Podaj poprawny adres e-mail.")
        return v
    }

    fun validatePassword(raw: String): String {
        if (raw.length < 6) throw ValidationException("Hasło musi mieć min. 6 znaków.")
        return raw
    }
}

object Money {
    fun format(minor: Long, currency: String = "PLN"): String {
        val nf = NumberFormat.getNumberInstance(Locale("pl", "PL")).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
        return "${nf.format(minor / 100.0)} $currency"
    }
}
