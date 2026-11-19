package com.example.turnosapp.auth.domain

import com.example.turnosapp.auth.domain.model.Registration
import com.example.turnosapp.auth.domain.model.RegistrationField

/**
 * Valida el formulario de registro antes de enviarlo, con las reglas de HU-01, para marcar los
 * errores sin esperar al backend. Es solo para la experiencia: el que decide es turnos, y lo que
 * esta validación deja pasar (por ejemplo, el patrón de caracteres del login) lo rechaza él.
 *
 * Nunca es más estricta que el backend: un dato que turnos acepta no puede rechazarse acá.
 */
class ValidateRegistrationUseCase {

    /** Devuelve el error de cada campo inválido; vacío si se puede enviar. */
    operator fun invoke(registration: Registration): Map<RegistrationField, String> {
        val errors = mutableMapOf<RegistrationField, String>()

        val login = registration.login.trim()
        if (login.isEmpty()) errors[RegistrationField.LOGIN] = REQUIRED
        else if (login.length > 50) errors[RegistrationField.LOGIN] = ruleOf(RegistrationField.LOGIN)

        // El máximo se mide en bytes y no en caracteres, como en el backend (ADR-0057): una letra
        // con tilde ocupa 2 bytes.
        val password = registration.password
        if (password.isEmpty()) errors[RegistrationField.PASSWORD] = REQUIRED
        else if (password.length < 4 || password.encodeToByteArray().size > 72) {
            errors[RegistrationField.PASSWORD] = ruleOf(RegistrationField.PASSWORD)
        }

        checkName(registration.firstName, RegistrationField.FIRST_NAME, errors)
        checkName(registration.lastName, RegistrationField.LAST_NAME, errors)

        val email = registration.email.trim()
        if (email.isEmpty()) errors[RegistrationField.EMAIL] = REQUIRED
        else if (email.length !in 5..254 || !EMAIL_SHAPE.matches(email)) {
            errors[RegistrationField.EMAIL] = ruleOf(RegistrationField.EMAIL)
        }

        if (registration.imageUrl.trim().length > 256) {
            errors[RegistrationField.IMAGE_URL] = ruleOf(RegistrationField.IMAGE_URL)
        }
        return errors
    }

    private fun checkName(value: String, field: RegistrationField, errors: MutableMap<RegistrationField, String>) {
        val name = value.trim()
        if (name.isEmpty()) errors[field] = REQUIRED
        else if (name.length !in 2..50) errors[field] = ruleOf(field)
    }

    companion object {
        private const val REQUIRED = "Este dato es obligatorio."

        // Solo la forma general (algo@algo): el backend hace la validación completa del email.
        private val EMAIL_SHAPE = Regex("^[^@\\s]+@[^@\\s]+$")

        /**
         * La regla de cada campo, dicha para el usuario. Se usa también para los `fieldErrors` del
         * backend, cuyos mensajes están en inglés y no se muestran.
         */
        fun ruleOf(field: RegistrationField): String =
            when (field) {
                RegistrationField.LOGIN -> "Hasta 50 caracteres: letras, números y . _ @ -"
                RegistrationField.PASSWORD -> "Entre 4 caracteres y 72 bytes (una letra con tilde ocupa 2)."
                RegistrationField.FIRST_NAME, RegistrationField.LAST_NAME -> "Entre 2 y 50 caracteres."
                RegistrationField.EMAIL -> "Ingresá un email válido."
                RegistrationField.IMAGE_URL -> "Hasta 256 caracteres."
            }
    }
}
