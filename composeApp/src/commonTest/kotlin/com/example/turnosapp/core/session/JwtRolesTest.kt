package com.example.turnosapp.core.session

import kotlin.test.Test
import kotlin.test.assertEquals

/** Los roles que la app lee del JWT para decidir qué secciones muestra (ADR-0082). */
class JwtRolesTest {

    @Test
    fun `lee la lista de roles del claim auth`() {
        val token = jwtExpiringAt(1L, roles = listOf("ROLE_USER", "ROLE_ADMIN"))

        assertEquals(listOf("ROLE_USER", "ROLE_ADMIN"), readJwtRoles(token))
    }

    @Test
    fun `sin claim auth no hay roles`() {
        assertEquals(emptyList(), readJwtRoles(jwtWithPayload("""{"sub":"ana","exp":1}""")))
    }

    @Test
    fun `un auth que no es una lista no da roles`() {
        assertEquals(emptyList(), readJwtRoles(jwtWithPayload("""{"sub":"ana","auth":"ROLE_ADMIN"}""")))
    }

    @Test
    fun `un token ilegible no da roles`() {
        assertEquals(emptyList(), readJwtRoles("no-es-un-jwt"))
    }
}
