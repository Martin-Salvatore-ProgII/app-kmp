package com.example.turnosapp.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.core.session.SessionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SessionAction {
    data object Logout : SessionAction
}

sealed interface SessionEffect {
    /** El usuario cerró la sesión. */
    data object LoggedOut : SessionEffect

    /** La sesión se cerró sola: un servicio respondió 401 a un pedido con el JWT. */
    data object Expired : SessionEffect
}

/**
 * La sesión vista desde la navegación de toda la app: con qué pantalla se arranca y cuándo hay que
 * volver a Login. Lo crea la navegación, no una pantalla, porque vale para todas.
 */
class SessionViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    /** Se lee una vez, al armar la navegación: con un JWT vigente se arranca en Buscar (ADR-0077). */
    val startsLoggedIn: Boolean = sessionRepository.isLoggedIn.value

    private val _effects = Channel<SessionEffect>(Channel.BUFFERED)
    val effects: Flow<SessionEffect> = _effects.receiveAsFlow()

    private var loggingOut = false

    init {
        viewModelScope.launch {
            var wasLoggedIn = sessionRepository.isLoggedIn.value
            sessionRepository.isLoggedIn.collect { loggedIn ->
                if (wasLoggedIn && !loggedIn) {
                    _effects.send(if (loggingOut) SessionEffect.LoggedOut else SessionEffect.Expired)
                    loggingOut = false
                }
                wasLoggedIn = loggedIn
            }
        }
    }

    fun onAction(action: SessionAction) {
        when (action) {
            SessionAction.Logout -> {
                loggingOut = true
                sessionRepository.clear()
            }
        }
    }
}
