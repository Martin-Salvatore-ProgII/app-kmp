package com.example.turnosapp.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.auth.data.AuthRepository
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.error.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Inicio de sesión (HU-02), con la opción de recordar la sesión 30 días (ADR-0033). */
class LoginViewModel(
    private val authRepository: AuthRepository,
    registeredLogin: String? = null,
    sessionExpired: Boolean = false,
) : ViewModel() {

    private val _state = MutableStateFlow(
        when {
            registeredLogin != null ->
                LoginUiState(username = registeredLogin, infoMessage = "Tu cuenta quedó creada. Ingresá con tu contraseña.")
            sessionExpired -> LoginUiState(infoMessage = AppError.Unauthorized.toUserMessage())
            else -> LoginUiState()
        },
    )
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.UsernameChanged ->
                _state.update { it.copy(username = action.value, errorMessage = null, invalidCredentials = false) }
            is LoginAction.PasswordChanged ->
                _state.update { it.copy(password = action.value, errorMessage = null, invalidCredentials = false) }
            is LoginAction.RememberMeChanged -> _state.update { it.copy(rememberMe = action.value) }
            LoginAction.PasswordVisibilityToggled -> _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            LoginAction.Submit -> login()
            LoginAction.RegisterClicked -> viewModelScope.launch { _effects.send(LoginEffect.NavigateToRegister) }
        }
    }

    private fun login() {
        val current = _state.value
        if (current.isLoading) return
        // Solo se evita un pedido que seguro falla; las credenciales las valida el backend.
        if (current.username.isBlank() || current.password.isEmpty()) {
            _state.update { it.copy(errorMessage = "Ingresá tu usuario y tu contraseña.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            authRepository.login(current.username, current.password, current.rememberMe)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, password = "") }
                    _effects.send(LoginEffect.LoggedIn)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = loginErrorMessage(error),
                            invalidCredentials = error == AppError.Unauthorized,
                        )
                    }
                }
        }
    }

    // En Login, un 401 no es una sesión vencida sino credenciales incorrectas. El backend no dice
    // cuál de los dos datos falló (HU-02, criterio 2), y la app tampoco.
    private fun loginErrorMessage(error: Throwable): String =
        if (error == AppError.Unauthorized) "Usuario o contraseña incorrectos." else error.toUserMessage()
}
