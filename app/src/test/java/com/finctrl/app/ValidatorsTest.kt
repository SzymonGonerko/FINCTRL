package com.finctrl.app

import com.finctrl.app.core.ValidationException
import com.finctrl.app.core.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidatorsTest {
    @Test fun `parses comma and dot decimals to minor units`() {
        assertEquals(1250L, Validators.parseAmount("12,50"))
        assertEquals(1250L, Validators.parseAmount(" 12.5 "))
        assertEquals(1250L, Validators.parseAmount("12.500"))
    }

    @Test fun `rejects invalid amounts`() {
        listOf("", "abc", "0", "-5", "1.234", "99999999999").forEach {
            assertThrows(it, ValidationException::class.java) { Validators.parseAmount(it) }
        }
    }

    @Test fun `pin must be 4 to 6 digits`() {
        assertEquals("1234", Validators.validatePin("1234"))
        listOf("123", "1234567", "12a4").forEach { assertThrows(ValidationException::class.java) { Validators.validatePin(it) } }
    }

    @Test fun `email and password validation`() {
        assertEquals("a@b.pl", Validators.validateEmail(" a@b.pl "))
        assertThrows(ValidationException::class.java) { Validators.validateEmail("a@b") }
        assertThrows(ValidationException::class.java) { Validators.validatePassword("12345") }
    }

    @Test fun `name is trimmed and length limited`() {
        assertEquals("Dom", Validators.requireName("  Dom "))
        assertThrows(ValidationException::class.java) { Validators.requireName("  ") }
        assertThrows(ValidationException::class.java) { Validators.requireName("x".repeat(41)) }
    }
}
