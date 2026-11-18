package com.example.turnosapp.auth.data.mapper

import com.example.turnosapp.auth.data.remote.dto.RegisterRequestDto
import com.example.turnosapp.auth.domain.model.Registration
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistrationMapperTest {

    @Test
    fun `quita espacios, manda la foto vacía como null y el idioma de la app`() {
        val dto = RegistrationMapper().toDto(
            Registration(" ana ", " clave ", " Ana ", " Pérez ", " ana@example.com ", "  "),
        )

        assertEquals(
            RegisterRequestDto(
                login = "ana",
                password = " clave ", // la contraseña no se toca
                firstName = "Ana",
                lastName = "Pérez",
                email = "ana@example.com",
                imageUrl = null,
                langKey = "es",
            ),
            dto,
        )
    }
}
