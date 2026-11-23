package com.example.turnosapp.admin.data

import com.example.turnosapp.admin.data.mapper.AdminReservationMapper
import com.example.turnosapp.admin.data.remote.AdminReservationApi
import com.example.turnosapp.admin.data.remote.dto.AdminCancelRequestDto
import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.admin.domain.model.AdminReservationFilters
import com.example.turnosapp.core.model.Page
import com.example.turnosapp.core.network.runCatchingApi

/** Cuántas reservas se piden por página; el mismo tamaño que Mis reservas. */
internal const val ADMIN_RESERVATIONS_PAGE_SIZE = 20

class AdminReservationRepositoryImpl(
    private val api: AdminReservationApi,
    private val mapper: AdminReservationMapper,
) : AdminReservationRepository {

    override suspend fun list(filters: AdminReservationFilters, page: Int): Result<Page<AdminReservation>> =
        runCatchingApi {
            val response = api.list(filters, page, ADMIN_RESERVATIONS_PAGE_SIZE)
            Page(response.items.map(mapper::toDomain), response.totalCount)
        }

    override suspend fun get(reservationId: String): Result<AdminReservation> =
        runCatchingApi { mapper.toDomain(api.get(reservationId)) }

    override suspend fun cancel(reservationId: String, reason: String): Result<AdminReservation> =
        runCatchingApi { mapper.toDomain(api.cancel(reservationId, AdminCancelRequestDto(reason.trim()))) }
}
