package com.example.turnosapp.reservation.data.mapper

import com.example.turnosapp.reservation.data.remote.dto.ReservationDto
import com.example.turnosapp.reservation.domain.model.Reservation
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/** Pasa una reserva de turnos al modelo de la app. Un estado que la app no conoce hace fallar la lectura. */
class ReservationMapper {

    fun toDomain(dto: ReservationDto): Reservation =
        Reservation(
            id = dto.id,
            status = ReservationStatus.valueOf(dto.status),
            professionalId = dto.professionalId,
            professionalFirstName = dto.professionalFirstName,
            professionalLastName = dto.professionalLastName,
            date = LocalDate.parse(dto.date),
            startTime = LocalTime.parse(dto.startTime),
            endTime = LocalTime.parse(dto.endTime),
            expiresAt = dto.expiresAt?.let(Instant::parse),
            message = dto.message,
            failureReason = dto.failureReason,
            createdAt = Instant.parse(dto.createdAt),
            cancelledAt = dto.cancelledAt?.let(Instant::parse),
            cancellationReason = dto.cancellationReason,
        )
}
