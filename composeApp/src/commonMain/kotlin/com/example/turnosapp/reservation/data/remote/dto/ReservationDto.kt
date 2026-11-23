package com.example.turnosapp.reservation.data.remote.dto

import kotlinx.serialization.Serializable

// Los cuerpos de las reservas de turnos (`docs/arq/contratos.md`, Reservas). Solo los campos que usa
// la app: `confirmedAt` no se muestra.

/** Las horas van como `18:00`; turnos acepta `HH:mm` y `HH:mm:ss`. */
@Serializable
data class StartReservationRequestDto(
    val professionalId: Long,
    val date: String,
    val startTime: String,
)

@Serializable
data class PhoneRequestDto(
    val phoneNumber: String,
)

/** Los instantes son ISO-8601 en UTC; las horas llegan como `18:00:00`. */
@Serializable
data class ReservationDto(
    val id: String,
    val status: String,
    val professionalId: Long,
    val professionalFirstName: String,
    val professionalLastName: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val expiresAt: String? = null,
    val message: String? = null,
    val failureReason: String? = null,
    val createdAt: String,
    val cancelledAt: String? = null,
    val cancellationReason: String? = null,
)
