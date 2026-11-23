package com.example.turnosapp.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

/**
 * Sesión respaldada por el almacenamiento cifrado del dispositivo (ADR-0049).
 *
 * El token se lee del almacenamiento una sola vez, al crear el contenedor, y después se guarda en
 * memoria: así no se descifra en cada pedido HTTP.
 *
 * @param nowEpochSeconds la hora actual; las pruebas pasan una fija.
 */
class SessionRepositoryImpl(
    private val tokenStorage: TokenStorage,
    nowEpochSeconds: () -> Long = { Clock.System.now().epochSeconds },
) : SessionRepository {

    private var token: String? = tokenStorage.read()

    init {
        // Al abrir la app: con un JWT vencido (o ilegible) se entra a Login, no a Buscar (ADR-0077).
        val expiration = token?.let(::readJwtExpiration)
        if (token != null && (expiration == null || expiration <= nowEpochSeconds())) {
            tokenStorage.clear()
            token = null
        }
    }

    private val _isLoggedIn = MutableStateFlow(token != null)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Se calcula al abrir, guardar y borrar la sesión: son los únicos momentos en que cambia el token.
    private val _isAdmin = MutableStateFlow(token.hasAdminRole())
    override val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    override fun currentToken(): String? = token

    override fun saveToken(token: String) {
        tokenStorage.save(token)
        this.token = token
        _isAdmin.value = token.hasAdminRole()
        _isLoggedIn.value = true
    }

    override fun clear() {
        tokenStorage.clear()
        token = null
        _isAdmin.value = false
        _isLoggedIn.value = false
    }

    private fun String?.hasAdminRole(): Boolean = this != null && ROLE_ADMIN in readJwtRoles(this)
}
