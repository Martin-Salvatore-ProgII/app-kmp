package com.example.turnosapp.admin.ui

import com.example.turnosapp.reservation.domain.model.ReservationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Los textos del bloque "Datos de soporte" (HU-10, criterio 2; ADR-0082). */
class AdminSupportLabelsTest {

    @Test
    fun `quién canceló distingue al dueño, a un administrador y a la cátedra`() {
        assertNull(adminReservation(ReservationStatus.CONFIRMED).cancelledByLabel())
        assertEquals("El usuario", adminReservation(ReservationStatus.CANCELLED, cancelledBy = "jperez").cancelledByLabel())
        assertEquals("El administrador admin", adminReservation(ReservationStatus.CANCELLED, cancelledBy = "admin").cancelledByLabel())
        assertEquals("La cátedra", adminReservation(ReservationStatus.CANCELLED, cancelledBy = "CATEDRA").cancelledByLabel())
    }

    @Test
    fun `el motivo de la falla lleva el mensaje y el código`() {
        val failed = adminReservation(ReservationStatus.FAILED, failureReason = "SLOT_ALREADY_HELD")

        assertEquals("Ese turno lo acababa de tomar otra persona (SLOT_ALREADY_HELD)", failed.failureLabel())
    }

    @Test
    fun `un código desconocido se muestra igual, con un texto genérico`() {
        val invalid = adminReservation(ReservationStatus.INVALID, failureReason = "NUEVO_CODIGO")

        assertEquals("Motivo informado por la cátedra (NUEVO_CODIGO)", invalid.failureLabel())
    }

    @Test
    fun `una reserva en curso no muestra motivo de falla aunque traiga el rechazo del teléfono`() {
        val awaiting = adminReservation(ReservationStatus.AWAITING_PHONE, failureReason = "INVALID_PHONE_NUMBER")

        assertNull(awaiting.failureLabel())
    }

    @Test
    fun `una vencida sin código explica que no se completó el teléfono`() {
        assertEquals("Venció sin completar el teléfono", adminReservation(ReservationStatus.EXPIRED).failureLabel())
    }

    @Test
    fun `sin proceso en la cátedra dice sin asignar`() {
        assertEquals("Sin asignar", adminReservation(ReservationStatus.FAILED, reservationProcessId = null).processLabel())
        assertEquals("8c2f6a1e", adminReservation(ReservationStatus.CONFIRMED).processLabel())
    }
}
