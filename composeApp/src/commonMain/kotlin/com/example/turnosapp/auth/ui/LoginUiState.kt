package com.example.turnosapp.auth.ui

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** El backend rechazó usuario y contraseña: se marcan los dos campos, sin decir cuál falló (HU-02). */
    val invalidCredentials: Boolean = false,
    /** Aviso que no es un error, por ejemplo que la cuenta se creó. */
    val infoMessage: String? = null,
)
