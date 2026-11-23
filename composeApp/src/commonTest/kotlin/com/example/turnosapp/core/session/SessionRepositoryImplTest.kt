package com.example.turnosapp.core.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Con qué sesión arranca la app según el JWT guardado (ADR-0077). */
class SessionRepositoryImplTest {

    private class InMemoryTokenStorage(var stored: String? = null) : TokenStorage {
        override fun read(): String? = stored
        override fun save(token: String) { stored = token }
        override fun clear() { stored = null }
    }

    private val now = 1_000_000L

    @Test
    fun `con un JWT vigente arranca con la sesión abierta`() {
        val token = jwtExpiringAt(now + 60)
        val repository = SessionRepositoryImpl(InMemoryTokenStorage(token)) { now }

        assertTrue(repository.isLoggedIn.value)
        assertEquals(token, repository.currentToken())
    }

    @Test
    fun `un JWT vencido se borra al arrancar`() {
        val storage = InMemoryTokenStorage(jwtExpiringAt(now))
        val repository = SessionRepositoryImpl(storage) { now }

        assertFalse(repository.isLoggedIn.value)
        assertNull(repository.currentToken())
        assertNull(storage.stored)
    }

    @Test
    fun `un token ilegible también se borra`() {
        val storage = InMemoryTokenStorage("no-es-un-jwt")

        assertFalse(SessionRepositoryImpl(storage) { now }.isLoggedIn.value)
        assertNull(storage.stored)
    }

    @Test
    fun `guardar y borrar el token abre y cierra la sesión`() {
        val storage = InMemoryTokenStorage()
        val repository = SessionRepositoryImpl(storage) { now }

        repository.saveToken("jwt")
        assertTrue(repository.isLoggedIn.value)
        assertEquals("jwt", storage.stored)

        repository.clear()
        assertFalse(repository.isLoggedIn.value)
        assertNull(storage.stored)
    }

    @Test
    fun `es administrador solo si el JWT trae ROLE_ADMIN`() {
        val admin = jwtExpiringAt(now + 60, roles = listOf("ROLE_USER", "ROLE_ADMIN"))
        val user = jwtExpiringAt(now + 60)

        assertTrue(SessionRepositoryImpl(InMemoryTokenStorage(admin)) { now }.isAdmin.value)
        assertFalse(SessionRepositoryImpl(InMemoryTokenStorage(user)) { now }.isAdmin.value)
    }

    @Test
    fun `el rol se actualiza al iniciar y al cerrar la sesión`() {
        val repository = SessionRepositoryImpl(InMemoryTokenStorage()) { now }

        repository.saveToken(jwtExpiringAt(now + 60, roles = listOf("ROLE_ADMIN")))
        assertTrue(repository.isAdmin.value)

        repository.clear()
        assertFalse(repository.isAdmin.value)

        repository.saveToken(jwtExpiringAt(now + 60))
        assertFalse(repository.isAdmin.value)
    }
}
