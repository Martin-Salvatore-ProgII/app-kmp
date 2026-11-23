package com.example.turnosapp.reservation.domain.model

import com.example.turnosapp.reservation.ui.reservation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/** El turno de prueba es el jueves 26 de noviembre a las 19:30 de Argentina, 22:30 en UTC. */
class ReservationTest {

    private val confirmed = reservation(ReservationStatus.CONFIRMED)

    @Test
    fun `una reserva confirmada se puede cancelar hasta que empieza el turno`() {
        assertTrue(confirmed.canBeCancelled(Instant.parse("2026-11-26T22:29:59Z")))
        assertFalse(confirmed.canBeCancelled(Instant.parse("2026-11-26T22:30:00Z")))
    }

    @Test
    fun `la hora del turno es la de Argentina, no la de UTC`() {
        // 19:45 en UTC son las 16:45 en Argentina: el turno de las 19:30 todavía no empezó.
        assertFalse(confirmed.hasStarted(Instant.parse("2026-11-26T19:45:00Z")))
    }

    @Test
    fun `solo se ofrece cancelar una reserva confirmada`() {
        val before = Instant.parse("2026-11-20T12:00:00Z")
        assertFalse(reservation(ReservationStatus.CANCELLED).canBeCancelled(before))
        assertFalse(reservation(ReservationStatus.AWAITING_PHONE).canBeCancelled(before))
        assertFalse(reservation(ReservationStatus.EXPIRED).canBeCancelled(before))
    }
}
