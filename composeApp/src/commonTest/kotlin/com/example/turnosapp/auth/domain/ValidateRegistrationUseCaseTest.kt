package com.example.turnosapp.auth.domain

import com.example.turnosapp.auth.domain.model.Registration
import com.example.turnosapp.auth.domain.model.RegistrationField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Las reglas de HU-01 que la app revisa antes de enviar el registro. */
class ValidateRegistrationUseCaseTest {

    private val validate = ValidateRegistrationUseCase()

    private val valid = Registration(
        login = "ana",
        password = "secreta",
        firstName = "Ana",
        lastName = "Pérez",
        email = "ana@example.com",
        imageUrl = "",
    )

    @Test
    fun `un registro válido no tiene errores`() {
        assertTrue(validate(valid).isEmpty())
    }

    @Test
    fun `los campos obligatorios vacíos se marcan, y la foto es opcional`() {
        val errors = validate(Registration("", "", " ", "", "", ""))

        assertEquals(
            setOf(
                RegistrationField.LOGIN,
                RegistrationField.PASSWORD,
                RegistrationField.FIRST_NAME,
                RegistrationField.LAST_NAME,
                RegistrationField.EMAIL,
            ),
            errors.keys,
        )
    }

    @Test
    fun `la contraseña tiene entre 4 caracteres y 72 bytes`() {
        assertTrue(RegistrationField.PASSWORD in validate(valid.copy(password = "abc")))
        assertTrue(validate(valid.copy(password = "a".repeat(72))).isEmpty())
        // 36 letras con tilde son 36 caracteres pero 72 bytes: entra justo. Una más ya no.
        assertTrue(validate(valid.copy(password = "á".repeat(36))).isEmpty())
        assertTrue(RegistrationField.PASSWORD in validate(valid.copy(password = "á".repeat(37))))
    }

    @Test
    fun `nombre y apellido tienen entre 2 y 50 caracteres sin contar espacios alrededor`() {
        assertTrue(RegistrationField.FIRST_NAME in validate(valid.copy(firstName = " A ")))
        assertTrue(RegistrationField.LAST_NAME in validate(valid.copy(lastName = "x".repeat(51))))
    }

    @Test
    fun `el email necesita la forma algo@algo`() {
        assertTrue(RegistrationField.EMAIL in validate(valid.copy(email = "ana.example.com")))
        assertTrue(validate(valid.copy(email = "ana@localhost")).isEmpty())
    }

    @Test
    fun `login y foto respetan su largo máximo`() {
        assertTrue(RegistrationField.LOGIN in validate(valid.copy(login = "a".repeat(51))))
        assertTrue(RegistrationField.IMAGE_URL in validate(valid.copy(imageUrl = "h".repeat(257))))
    }
}
