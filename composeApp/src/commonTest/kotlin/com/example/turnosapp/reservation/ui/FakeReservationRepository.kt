package com.example.turnosapp.reservation.ui

import com.example.turnosapp.reservation.data.ReservationRepository
import com.example.turnosapp.reservation.domain.model.Reservation
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/** Devuelve lo que el escenario necesita y cuenta los pedidos. */
open class FakeReservationRepository : ReservationRepository {
    var startResult: Result<Reservation> = Result.success(reservation(ReservationStatus.AWAITING_REQUEST))
    var activeResult: Result<Reservation?> = Result.success(null)
    var startCalls = 0
    var getResult: Result<Reservation> = Result.success(reservation(ReservationStatus.AWAITING_REQUEST))
    var getCalls = 0
    var phoneResult: Result<Reservation> = Result.success(reservation(ReservationStatus.PHONE_SUBMITTED))
    val submittedPhones = mutableListOf<String>()

    override suspend fun start(professionalId: Long, date: LocalDate, startTime: LocalTime): Result<Reservation> {
        startCalls++
        return startResult
    }

    override suspend fun get(reservationId: String): Result<Reservation> {
        getCalls++
        return getResult
    }

    override suspend fun submitPhone(reservationId: String, phoneNumber: String): Result<Reservation> {
        submittedPhones += phoneNumber
        return phoneResult
    }

    override suspend fun findActive(): Result<Reservation?> = activeResult
}

/** Una reserva de prueba con Martín Rivas el jueves 26 a las 19:30. */
fun reservation(
    status: ReservationStatus,
    id: String = "5b1f0c2e-8d7a-4a51-9e0b-1f2d3c4b5a69",
    expiresAt: Instant? = Instant.parse("2026-11-19T14:44:50Z"),
    message: String? = null,
    failureReason: String? = null,
) = Reservation(
    id = id,
    status = status,
    professionalId = 202,
    professionalFirstName = "Martin",
    professionalLastName = "Rivas",
    date = LocalDate(2026, 11, 26),
    startTime = LocalTime(19, 30),
    endTime = LocalTime(20, 0),
    expiresAt = expiresAt,
    message = message,
    failureReason = failureReason,
)
