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
}
