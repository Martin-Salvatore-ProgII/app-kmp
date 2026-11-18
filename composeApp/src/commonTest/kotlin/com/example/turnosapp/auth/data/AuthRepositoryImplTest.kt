package com.example.turnosapp.auth.data

import com.example.turnosapp.auth.data.mapper.RegistrationMapper
import com.example.turnosapp.auth.data.remote.AuthApi
import com.example.turnosapp.auth.domain.model.Registration
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.createHttpClient
import com.example.turnosapp.core.session.FakeSessionRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El repositorio con el cliente HTTP real contra un turnos simulado: el contrato de login (HU-02). */
class AuthRepositoryImplTest {

    private val session = FakeSessionRepository()
    private var lastRequest: HttpRequestData? = null

    private fun repositoryAnswering(status: HttpStatusCode, body: String): AuthRepository {
        val engine = MockEngine { request ->
            lastRequest = request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = AuthApi(createHttpClient(engine, session), ServiceUrls(turnos = "https://turnos", catalog = "https://catalogo"))
        return AuthRepositoryImpl(api, session, RegistrationMapper())
    }

    @Test
    fun `un registro aceptado con 201 sin cuerpo es un éxito y no inicia sesión`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.Created, "")

        val result = repository.register(Registration("ana", "secreta", "Ana", "Pérez", "ana@example.com", ""))

        assertTrue(result.isSuccess)
        assertEquals("https://turnos/api/register", lastRequest!!.url.toString())
        assertNull(session.token)
    }

    @Test
    fun `un login correcto guarda el id_token en la sesión`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, """{"id_token":"jwt-de-prueba"}""")

        val result = repository.login(" ana ", "secreta", rememberMe = true)

        assertTrue(result.isSuccess)
        assertEquals("jwt-de-prueba", session.token)
        assertEquals("https://turnos/api/authenticate", lastRequest!!.url.toString())
        // Los nombres de campo son los del contrato, y el usuario va sin espacios alrededor.
        val body = (lastRequest!!.body as TextContent).text
        assertEquals("""{"username":"ana","password":"secreta","rememberMe":true}""", body)
    }

    @Test
    fun `credenciales incorrectas llegan como Unauthorized y no guardan nada`() = runTest {
        // Turnos responde 401 sin code, igual para un usuario inexistente o una contraseña incorrecta.
        val repository = repositoryAnswering(HttpStatusCode.Unauthorized, "")

        val result = repository.login("ana", "mala", rememberMe = false)

        assertEquals(AppError.Unauthorized, result.exceptionOrNull())
        assertNull(session.token)
    }
}
