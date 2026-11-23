package com.example.turnosapp.admin.ui

import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.core.error.adminFailureDescription
import com.example.turnosapp.reservation.domain.model.ReservationStatus

// Los textos del bloque "Datos de soporte" del detalle admin (HU-10, criterio 2; ADR-0082).

/** El valor que turnos guarda cuando la cancelación vino de la cátedra (`docs/arq/modelo-db.md`). */
private const val CANCELLED_BY_CATEDRA = "CATEDRA"

/** Quién canceló la reserva: el dueño, un administrador o la cátedra; null si no está cancelada. */
fun AdminReservation.cancelledByLabel(): String? =
    when (cancelledBy) {
        null -> null
        CANCELLED_BY_CATEDRA -> "La cátedra"
        owner.login -> "El usuario"
        else -> "El administrador $cancelledBy"
    }

/**
 * Por qué no se completó, solo en los estados que terminaron sin reservar. En una reserva en curso el
 * `failureReason` puede traer el rechazo de un teléfono, que no es una falla del proceso.
 */
fun AdminReservation.failureLabel(): String? {
    val status = reservation.status
    if (status != ReservationStatus.FAILED && status != ReservationStatus.EXPIRED && status != ReservationStatus.INVALID) {
        return null
    }
    return reservation.failureReason?.let(::adminFailureDescription)
        ?: if (status == ReservationStatus.EXPIRED) "Venció sin completar el teléfono" else "Sin motivo informado"
}

/** El proceso en la cátedra; una reserva cuyo hold nunca se creó no tiene. */
fun AdminReservation.processLabel(): String = reservationProcessId ?: "Sin asignar"
