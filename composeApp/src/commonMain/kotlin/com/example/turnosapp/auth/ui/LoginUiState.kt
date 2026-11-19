package com.example.turnosapp.auth.ui

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Aviso que no es un error, por ejemplo que la cuenta se creó. */
    val infoMessage: String? = null,
)
