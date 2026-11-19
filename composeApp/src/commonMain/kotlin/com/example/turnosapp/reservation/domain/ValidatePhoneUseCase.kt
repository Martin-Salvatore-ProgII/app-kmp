package com.example.turnosapp.reservation.domain

/**
 * Valida un teléfono con la regla de la cátedra (REF §15.5, ADR-0027): se quitan espacios, guiones,
 * paréntesis y puntos, y lo que queda tiene que ser `^\+?[0-9]{7,15}$`.
 *
 * Es la excepción de la Constitución P-21: una regla fija de un contrato externo, que la app repite
 * solo para avisar al instante y no gastar tiempo del hold. Turnos la vuelve a aplicar, y el teléfono
 * se envía como lo escribió el usuario: lo normaliza el backend.
 */
class ValidatePhoneUseCase {

    operator fun invoke(phone: String): Boolean = PHONE_PATTERN.matches(phone.replace(SEPARATORS, ""))

    private companion object {
        val SEPARATORS = Regex("[\\s\\-().]")
        val PHONE_PATTERN = Regex("^\\+?[0-9]{7,15}$")
    }
}
