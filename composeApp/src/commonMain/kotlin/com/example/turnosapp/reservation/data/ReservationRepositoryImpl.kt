package com.example.turnosapp.reservation.data

import com.example.turnosapp.core.network.runCatchingApi
import com.example.turnosapp.reservation.data.mapper.ReservationMapper
import com.example.turnosapp.reservation.data.remote.ReservationApi
import com.example.turnosapp.reservation.data.remote.dto.PhoneRequestDto
import com.example.turnosapp.reservation.data.remote.dto.StartReservationRequestDto
import com.example.turnosapp.reservation.domain.model.Reservation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class ReservationRepositoryImpl(
    private val api: ReservationApi,
    private val mapper: ReservationMapper,
) : ReservationRepository {

    override suspend fun start(professionalId: Long, date: LocalDate, startTime: LocalTime): Result<Reservation> =
        runCatchingApi {
            mapper.toDomain(api.start(StartReservationRequestDto(professionalId, date.toString(), startTime.toString())))
        }

    override suspend fun get(reservationId: String): Result<Reservation> =
        runCatchingApi { mapper.toDomain(api.get(reservationId)) }

    override suspend fun submitPhone(reservationId: String, phoneNumber: String): Result<Reservation> =
        runCatchingApi { mapper.toDomain(api.submitPhone(reservationId, PhoneRequestDto(phoneNumber))) }

    // Un usuario tiene a lo sumo un proceso activo, y mientras lo tiene no puede crear otro (ADR-0024):
    // si existe, es el último creado. Turnos ordena por fecha de creación, del más nuevo al más viejo.
    override suspend fun findActive(): Result<Reservation?> =
        runCatchingApi {
            api.listLatest(size = 1).firstOrNull()?.let(mapper::toDomain)?.takeUnless { it.status.isFinal }
        }
}
