package com.example.turnosapp.core.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtExpirationTest {

    @Test
    fun `lee el exp del JWT`() {
        assertEquals(1_800_000_000L, readJwtExpiration(jwtExpiringAt(1_800_000_000L)))
    }

    @Test
    fun `un token que no es un JWT no tiene vencimiento legible`() {
        assertNull(readJwtExpiration("cualquier-cosa"))
        assertNull(readJwtExpiration("a.%%%.c"))
        assertNull(readJwtExpiration("a.bm8tZXMtanNvbg.c")) // "no-es-json" en Base64
    }
}
