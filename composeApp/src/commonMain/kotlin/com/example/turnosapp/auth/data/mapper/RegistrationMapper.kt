package com.example.turnosapp.auth.data.mapper

import com.example.turnosapp.auth.data.remote.dto.RegisterRequestDto
import com.example.turnosapp.auth.domain.model.Registration

class RegistrationMapper {

    fun toDto(registration: Registration): RegisterRequestDto =
        RegisterRequestDto(
            login = registration.login.trim(),
            // La contraseña va tal cual: un espacio al principio o al final es parte de ella.
            password = registration.password,
            firstName = registration.firstName.trim(),
            lastName = registration.lastName.trim(),
            email = registration.email.trim(),
            imageUrl = registration.imageUrl.trim().ifEmpty { null },
            langKey = APP_LANGUAGE,
        )

    private companion object {
        // El contrato pide el idioma del usuario. La app está solo en español, así que no se le
        // pregunta: elegir otro idioma no cambiaría nada de lo que ve.
        const val APP_LANGUAGE = "es"
    }
}
