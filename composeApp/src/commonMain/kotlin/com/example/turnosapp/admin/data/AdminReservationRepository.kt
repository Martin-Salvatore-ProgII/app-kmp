package com.example.turnosapp.admin.data

import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.admin.domain.model.AdminReservationFilters
import com.example.turnosapp.core.model.Page

/**
 * Las reservas de todos los usuarios, para un administrador (HU-10). Un usuario sin `ROLE_ADMIN`
 * recibe 403, que llega como `AppError.Forbidden`.
 */
interface AdminReservationRepository {

    /**
     * Una página de todas las reservas que cumplen los filtros, de la más nueva a la más vieja.
     *
     * @param page la página, desde 0.
     */
    suspend fun list(filters: AdminReservationFilters, page: Int): Result<Page<AdminReservation>>

    /** Cualquier reserva, por su id local. Una inexistente llega como `RESERVATION_NOT_FOUND`. */
    suspend fun get(reservationId: String): Result<AdminReservation>

    /**
     * Cancela una reserva confirmada en nombre de su dueño (HU-10). Las mismas reglas que la
     * cancelación del dueño: `RESERVATION_NOT_CANCELLABLE` llega como `AppError.Business` y la cátedra
     * caída como `AppError.Unavailable`. Turnos responde con `cancelledBy` = el login del administrador.
     *
     * @param reason el motivo, obligatorio; se envía sin los espacios de los extremos.
     */
    suspend fun cancel(reservationId: String, reason: String): Result<AdminReservation>
}
