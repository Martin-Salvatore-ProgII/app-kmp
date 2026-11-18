package com.example.turnosapp.auth.ui

import com.example.turnosapp.auth.domain.model.Registration
import com.example.turnosapp.auth.domain.model.RegistrationField

data class RegisterUiState(
    val form: Registration = Registration(
        login = "",
        password = "",
        firstName = "",
        lastName = "",
        email = "",
        imageUrl = "",
    ),
    /** El error de cada campo marcado, sea de la validación local o de los `fieldErrors` del backend. */
    val fieldErrors: Map<RegistrationField, String> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
