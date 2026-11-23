package com.example.turnosapp.admin.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * La vista administrativa de una reserva (`docs/arq/contratos.md`, Administración). Turnos manda los
 * campos de la reserva al mismo nivel que los de soporte, por eso se repiten acá en lugar de anidar un
 * `ReservationDto`. Solo los campos que usa la app.
 */
@Serializable
data class AdminReservationDto(
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
    val owner: ReservationOwnerDto,
    val reservationProcessId: String? = null,
    val cancelledBy: String? = null,
)

@Serializable
data class ReservationOwnerDto(
    val login: String,
    val firstName: String,
    val lastName: String,
)

/** El motivo es obligatorio para un administrador: de 1 a 500 caracteres y no en blanco (HU-10). */
@Serializable
data class AdminCancelRequestDto(
    val reason: String,
)
