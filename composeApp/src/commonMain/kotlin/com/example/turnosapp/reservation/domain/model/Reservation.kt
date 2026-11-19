package com.example.turnosapp.reservation.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/**
 * Un proceso de reserva, tal como lo informa turnos (`docs/arq/contratos.md`, Reservas).
 *
 * @param expiresAt el vencimiento que fijó la cátedra; solo tiene sentido mientras no es final.
 * @param message el último mensaje de la cátedra para el usuario (el pedido o el rechazo del teléfono).
 * @param failureReason un código estable para elegir el mensaje propio; nunca se muestra tal cual.
 */
data class Reservation(
    val id: String,
    val status: ReservationStatus,
    val professionalId: Long,
    val professionalFirstName: String,
    val professionalLastName: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val expiresAt: Instant?,
    val message: String?,
    val failureReason: String?,
) {
    val professionalFullName: String get() = "$professionalFirstName $professionalLastName"
}

/** Los estados de `docs/arq/maquina-estados.md`. */
enum class ReservationStatus {
    STARTED,
    HELD,
    AWAITING_REQUEST,
    AWAITING_PHONE,
    PHONE_SUBMITTED,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    INVALID,
    FAILED,
    ;

    /** Un estado final no cambia más (salvo confirmado → cancelado): la app deja de consultarlo. */
    val isFinal: Boolean get() = this in setOf(CONFIRMED, CANCELLED, EXPIRED, INVALID, FAILED)
}
