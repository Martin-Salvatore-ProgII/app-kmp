package com.example.turnosapp.reservation.data

import com.example.turnosapp.reservation.domain.model.Reservation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Los procesos de reserva del usuario, en turnos. Siempre los del usuario del JWT (RNF-03). */
interface ReservationRepository {

    /**
     * Inicia una reserva: turnos crea el hold y lo confirma en un solo paso (ADR-0019). Responde con
     * el proceso aunque la cátedra lo haya rechazado: llega en `FAILED` con su motivo (ADR-0069).
     * `ACTIVE_RESERVATION_EXISTS` llega como `AppError.Business` (ADR-0024).
     */
    suspend fun start(professionalId: Long, date: LocalDate, startTime: LocalTime): Result<Reservation>

    /**
     * Un proceso propio, por su id. Es la operación que Reserva en curso consulta cada pocos segundos
     * (ADR-0021). Uno inexistente o de otro usuario llega como `RESERVATION_NOT_FOUND`.
     */
    suspend fun get(reservationId: String): Result<Reservation>

    /**
     * Envía el teléfono que pidió la cátedra. Turnos responde con el proceso en `PHONE_SUBMITTED`.
     * `INVALID_PHONE_NUMBER` y `PHONE_NOT_REQUESTED` (el proceso ya no lo espera) llegan como `AppError.Business`.
     */
    suspend fun submitPhone(reservationId: String, phoneNumber: String): Result<Reservation>

    /** El proceso activo del usuario, o null si no tiene ninguno (ADR-0080). */
    suspend fun findActive(): Result<Reservation?>
}
