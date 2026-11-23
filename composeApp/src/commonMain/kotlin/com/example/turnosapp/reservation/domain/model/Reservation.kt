package com.example.turnosapp.reservation.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/**
 * Un proceso de reserva, tal como lo informa turnos (`docs/arq/contratos.md`, Reservas).
 *
 * @param expiresAt el vencimiento que fijó la cátedra; solo tiene sentido mientras no es final.
 * @param message el último mensaje de la cátedra para el usuario (el pedido o el rechazo del teléfono).
 * @param failureReason un código estable para elegir el mensaje propio; nunca se muestra tal cual.
 * @param createdAt cuándo se inició; Mis reservas lo muestra como "Reservada el…".
 * @param cancellationReason el motivo opcional que escribió quien la canceló.
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
    val createdAt: Instant,
    val cancelledAt: Instant? = null,
    val cancellationReason: String? = null,
) {
    val professionalFullName: String get() = "$professionalFirstName $professionalLastName"

    /**
     * Si el turno ya empezó en `now`. La fecha y las horas son de Argentina (ADR-0016), no de la zona
     * del teléfono, igual que en turnos.
     */
    fun hasStarted(now: Instant): Boolean = date.atTime(startTime).toInstant(BUSINESS_TIME_ZONE) <= now

    /**
     * Si la app ofrece cancelarla: confirmada y con el turno por delante (ADR-0071, ADR-0081). Es
     * solo para no mostrar un botón que siempre falla: la autoridad es turnos, que responde 409 si no.
     */
    fun canBeCancelled(now: Instant): Boolean = status == ReservationStatus.CONFIRMED && !hasStarted(now)
}

/** La zona de la agenda y de los turnos (ADR-0016). */
val BUSINESS_TIME_ZONE: TimeZone = TimeZone.of("America/Argentina/Buenos_Aires")

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
