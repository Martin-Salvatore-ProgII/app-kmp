package com.example.turnosapp.core.session

/** Sesión en memoria para las pruebas: guarda el token en una variable. */
class FakeSessionRepository(var token: String? = null) : SessionRepository {

    override fun currentToken(): String? = token

    override fun saveToken(token: String) {
        this.token = token
    }

    override fun clear() {
        token = null
    }
}
