package com.example.turnosapp.admin.data.remote

import com.example.turnosapp.admin.data.remote.dto.AdminReservationDto
import com.example.turnosapp.admin.domain.model.AdminReservationFilters
import com.example.turnosapp.core.network.PagedResponse
import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.toPagedResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Las operaciones administrativas de turnos (`docs/arq/contratos.md`, Administración). Turnos las
 * protege con `ROLE_ADMIN`: sin ese rol responde 403 (ADR-0037).
 */
class AdminReservationApi(
    private val client: HttpClient,
    private val urls: ServiceUrls,
) {
    /** Una página de todas las reservas, con el orden por defecto de turnos: las más nuevas primero. */
    suspend fun list(filters: AdminReservationFilters, page: Int, size: Int): PagedResponse<AdminReservationDto> =
        client.get("${urls.turnos}/api/admin/reservations") {
            filters.ownerLogin?.let { parameter("ownerLogin", it) }
            filters.status?.let { parameter("status", it.name) }
            filters.from?.let { parameter("from", it.toString()) }
            filters.to?.let { parameter("to", it.toString()) }
            parameter("page", page)
            parameter("size", size)
        }.toPagedResponse(page, size)
}
