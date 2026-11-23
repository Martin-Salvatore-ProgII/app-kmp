package com.example.turnosapp.admin.data.mapper

import com.example.turnosapp.admin.data.remote.dto.AdminReservationDto
import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.admin.domain.model.ReservationOwner
import com.example.turnosapp.reservation.data.mapper.ReservationMapper
import com.example.turnosapp.reservation.data.remote.dto.ReservationDto

/**
 * Pasa la vista administrativa de una reserva al modelo de la app. La parte de la reserva la lee el
 * mismo `ReservationMapper` que usa Mis reservas, así las fechas y los estados se leen en un solo lugar.
 */
class AdminReservationMapper(
    private val reservationMapper: ReservationMapper,
) {
    fun toDomain(dto: AdminReservationDto): AdminReservation =
        AdminReservation(
            reservation = reservationMapper.toDomain(dto.toReservationDto()),
            owner = ReservationOwner(dto.owner.login, dto.owner.firstName, dto.owner.lastName),
            reservationProcessId = dto.reservationProcessId,
            cancelledBy = dto.cancelledBy,
        )

    private fun AdminReservationDto.toReservationDto(): ReservationDto =
        ReservationDto(
            id = id,
            status = status,
            professionalId = professionalId,
            professionalFirstName = professionalFirstName,
            professionalLastName = professionalLastName,
            date = date,
            startTime = startTime,
            endTime = endTime,
            expiresAt = expiresAt,
            message = message,
            failureReason = failureReason,
            createdAt = createdAt,
            cancelledAt = cancelledAt,
            cancellationReason = cancellationReason,
        )
}
