package com.example.turnosapp.core.network

import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.session.FakeSessionRepository
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El cliente real de la app (`createHttpClient`) contra un backend simulado con MockEngine:
 * prueba que una respuesta HTTP de verdad llega al ViewModel como resultado o como AppError.
 */
class RunCatchingApiTest {

    @Serializable
    private data class Greeting(val text: String)

    private fun clientAnswering(status: HttpStatusCode, body: String, contentType: String = "application/problem+json") =
        createHttpClient(
            MockEngine {
                respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, contentType))
            },
            FakeSessionRepository(),
        )

    @Test
    fun `una respuesta 200 devuelve el cuerpo`() = runTest {
        val client = clientAnswering(HttpStatusCode.OK, """{"text":"hola","otroCampo":1}""", "application/json")

        val result = runCatchingApi { client.get("https://backend/saludo").body<Greeting>() }

        // El campo desconocido se ignora: un campo nuevo del backend no rompe la app.
        assertEquals(Greeting("hola"), result.getOrThrow())
    }

    @Test
    fun `un problem+json con code se traduce a Business`() = runTest {
        val client = clientAnswering(
            HttpStatusCode.Conflict,
            """{"type":"about:blank","title":"Conflict","status":409,"detail":"texto libre","code":"ACTIVE_RESERVATION_EXISTS"}""",
        )

        val result = runCatchingApi { client.get("https://backend/reservas").body<Greeting>() }

        assertEquals(AppError.Business(409, "ACTIVE_RESERVATION_EXISTS"), result.exceptionOrNull())
    }

    @Test
    fun `un 400 con fieldErrors se traduce a Validation`() = runTest {
        val client = clientAnswering(
            HttpStatusCode.BadRequest,
            """{"status":400,"code":"VALIDATION_ERROR","fieldErrors":[{"objectName":"user","field":"email","message":"inválido"}]}""",
        )

        val result = runCatchingApi { client.get("https://backend/registro").body<Greeting>() }

        assertEquals(AppError.Validation(mapOf("email" to "inválido")), result.exceptionOrNull())
    }

    @Test
    fun `un 401 sin cuerpo se traduce a Unauthorized`() = runTest {
        val client = clientAnswering(HttpStatusCode.Unauthorized, "")

        val result = runCatchingApi { client.get("https://backend/reservas").body<Greeting>() }

        assertEquals(AppError.Unauthorized, result.exceptionOrNull())
    }

    @Test
    fun `un 500 con un cuerpo que no es JSON se traduce a Unknown`() = runTest {
        val client = clientAnswering(HttpStatusCode.InternalServerError, "<html>error</html>", "text/html")

        val result = runCatchingApi { client.get("https://backend/reservas").body<Greeting>() }

        assertEquals(AppError.Unknown, result.exceptionOrNull())
    }

    @Test
    fun `una falla de conexion se traduce a Network`() = runTest {
        val client = createHttpClient(MockEngine { throw IOException("sin conexión") }, FakeSessionRepository())

        val result = runCatchingApi { client.get("https://backend/reservas").body<Greeting>() }

        assertEquals(AppError.Network, result.exceptionOrNull())
    }
}
