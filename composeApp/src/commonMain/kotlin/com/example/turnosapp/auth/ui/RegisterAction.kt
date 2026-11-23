package com.example.turnosapp.auth.ui

import com.example.turnosapp.auth.domain.model.RegistrationField

sealed interface RegisterAction {
    data class FieldChanged(val field: RegistrationField, val value: String) : RegisterAction
    data object PasswordVisibilityToggled : RegisterAction
    data object ImageUrlExpanded : RegisterAction
    data object Submit : RegisterAction
    data object BackClicked : RegisterAction
}

sealed interface RegisterEffect {
    /** La cuenta quedó creada: se vuelve a Login con el usuario ya escrito. */
    data class Registered(val login: String) : RegisterEffect
    data object NavigateBack : RegisterEffect
}
