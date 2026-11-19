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
    val passwordVisible: Boolean = false,
    /** La foto es opcional: su campo queda oculto hasta que el usuario lo pide (ADR-0079). */
    val imageUrlExpanded: Boolean = false,
) {
    /** Se ve si se pidió, si ya tiene algo escrito o si el backend lo marcó con un error. */
    val showImageUrl: Boolean
        get() = imageUrlExpanded || form.imageUrl.isNotEmpty() || RegistrationField.IMAGE_URL in fieldErrors
}
