package com.example.turnosapp.core.network

import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.session.FakeSessionRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El JWT en cada pedido y el 401 que cierra la sesión, en un solo lugar (`docs/arq/interfaz.md`). */
class SessionPluginTest {

    private var sentAuthorization: String? = null

    private fun client(session: FakeSessionRepository, status: HttpStatusCode) =
        createHttpClient(
            MockEngine { request ->
                sentAuthorization = request.headers[HttpHeaders.Authorization]
                respond("", status)
            },
            session,
        )

    @Test
    fun `con sesión, cada pedido lleva el JWT`() = runTest {
        val session = FakeSessionRepository("jwt-de-prueba")

        client(session, HttpStatusCode.OK).get("https://catalogo/api/categories")

        assertEquals("Bearer jwt-de-prueba", sentAuthorization)
    }

    @Test
    fun `sin sesión, el pedido va sin Authorization`() = runTest {
        client(FakeSessionRepository(), HttpStatusCode.OK).get("https://turnos/api/authenticate")

        assertNull(sentAuthorization)
    }

    @Test
    fun `un 401 de un pedido con JWT cierra la sesión`() = runTest {
        val session = FakeSessionRepository("jwt-vencido")
        val client = client(session, HttpStatusCode.Unauthorized)

        val result = runCatchingApi { client.get("https://turnos/api/reservations") }

        assertEquals(AppError.Unauthorized, result.exceptionOrNull())
        assertFalse(session.isLoggedIn.value)
        assertNull(session.token)
    }

    @Test
    fun `un 200 no toca la sesión`() = runTest {
        val session = FakeSessionRepository("jwt")

        client(session, HttpStatusCode.OK).get("https://turnos/api/reservations")

        assertTrue(session.isLoggedIn.value)
    }
}
