package com.example.turnosapp.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Sesión en memoria para las pruebas: guarda el token en una variable. */
class FakeSessionRepository(token: String? = null, isAdmin: Boolean = false) : SessionRepository {

    var token: String? = token
        private set

    private val _isLoggedIn = MutableStateFlow(token != null)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isAdmin = MutableStateFlow(isAdmin)
    override val isAdmin: StateFlow<Boolean> = _isAdmin

    override fun currentToken(): String? = token

    override fun saveToken(token: String) {
        this.token = token
        _isLoggedIn.value = true
    }

    override fun clear() {
        token = null
        _isAdmin.value = false
        _isLoggedIn.value = false
    }
}
