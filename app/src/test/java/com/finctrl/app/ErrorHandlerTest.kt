package com.finctrl.app

import com.finctrl.app.core.ErrorHandler
import com.finctrl.app.core.ValidationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class ErrorHandlerTest {
    private val handler = ErrorHandler()

    @Test fun `validation message passes through`() =
        assertEquals("Podaj poprawną kwotę.", handler.toMessage(ValidationException("Podaj poprawną kwotę.")))

    @Test fun `network errors get friendly message`() =
        assertTrue(handler.toMessage(UnknownHostException()).contains("połączenia"))

    @Test fun `unknown error falls back to generic`() =
        assertEquals("Wystąpił nieoczekiwany błąd.", handler.toMessage(RuntimeException()))
}
