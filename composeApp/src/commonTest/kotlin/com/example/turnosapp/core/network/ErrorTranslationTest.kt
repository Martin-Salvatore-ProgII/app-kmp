package com.example.turnosapp.core.network

import com.example.turnosapp.core.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals

/** La tabla de errores de `docs/arq/interfaz.md`, caso por caso. */
class ErrorTranslationTest {

    @Test
    fun `503 CATALOG_NOT_READY es el único 503 que se distingue`() {
        assertEquals(AppError.CatalogNotReady, toAppError(503, ProblemDto(code = "CATALOG_NOT_READY")))
        assertEquals(AppError.Unavailable, toAppError(503, ProblemDto(code = "CATALOG_UNAVAILABLE")))
    }

    @Test
    fun `401 es Unauthorized, traiga o no cuerpo`() {
        assertEquals(AppError.Unauthorized, toAppError(401, null))
        assertEquals(AppError.Unauthorized, toAppError(401, ProblemDto(code = "CUALQUIERA")))
    }

    @Test
    fun `403 es Forbidden aunque no traiga code`() {
        assertEquals(AppError.Forbidden, toAppError(403, null))
    }

    @Test
    fun `400 con fieldErrors es Validation con un mensaje por campo`() {
        val problem = ProblemDto(
            code = "VALIDATION_ERROR",
            fieldErrors = listOf(
                FieldErrorDto(field = "email", message = "formato inválido"),
                FieldErrorDto(field = "login", message = "obligatorio"),
                FieldErrorDto(field = "login", message = "demasiado largo"),
            ),
        )

        val error = toAppError(400, problem)

        assertEquals(AppError.Validation(mapOf("email" to "formato inválido", "login" to "obligatorio")), error)
    }

    @Test
    fun `400 con code y sin fieldErrors es Business`() {
        assertEquals(
            AppError.Business(400, "USERNAME_ALREADY_EXISTS"),
            toAppError(400, ProblemDto(code = "USERNAME_ALREADY_EXISTS")),
        )
    }

    @Test
    fun `404 y 409 con code son Business con su status`() {
        assertEquals(AppError.Business(404, "RESERVATION_NOT_FOUND"), toAppError(404, ProblemDto(code = "RESERVATION_NOT_FOUND")))
        assertEquals(AppError.Business(409, "ACTIVE_RESERVATION_EXISTS"), toAppError(409, ProblemDto(code = "ACTIVE_RESERVATION_EXISTS")))
    }

    @Test
    fun `503 es Unavailable, con o sin code`() {
        assertEquals(AppError.Unavailable, toAppError(503, ProblemDto(code = "INTEGRATION_UNAVAILABLE")))
        assertEquals(AppError.Unavailable, toAppError(503, null))
    }

    @Test
    fun `4xx sin code y 500 son Unknown`() {
        assertEquals(AppError.Unknown, toAppError(404, null))
        assertEquals(AppError.Unknown, toAppError(500, ProblemDto(code = "ALGO")))
    }
}
