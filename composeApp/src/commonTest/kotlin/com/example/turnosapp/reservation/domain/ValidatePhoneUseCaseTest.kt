package com.example.turnosapp.reservation.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** La regla de la cátedra (REF §15.5): sin separadores, `+` opcional y de 7 a 15 dígitos. */
class ValidatePhoneUseCaseTest {

    private val validatePhone = ValidatePhoneUseCase()

    @Test
    fun `acepta un teléfono con espacios, guiones, paréntesis y puntos`() {
        assertTrue(validatePhone("+54 9 (11) 5555-1234"))
        assertTrue(validatePhone("11.5555.1234"))
        assertTrue(validatePhone("1234567"))
        assertTrue(validatePhone("+123456789012345"))
    }

    @Test
    fun `rechaza lo que no cumple la regla`() {
        assertFalse(validatePhone("11 5555"))
        assertFalse(validatePhone("1234567890123456"))
        assertFalse(validatePhone("11-5555-abcd"))
        assertFalse(validatePhone("54+1155551234"))
        assertFalse(validatePhone(""))
    }
}
