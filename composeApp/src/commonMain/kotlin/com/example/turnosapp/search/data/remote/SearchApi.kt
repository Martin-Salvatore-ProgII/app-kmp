package com.example.turnosapp.search.data.remote

import com.example.turnosapp.core.network.PagedResponse
import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.toPagedResponse
import com.example.turnosapp.search.data.remote.dto.CategoryDto
import com.example.turnosapp.search.data.remote.dto.ProfessionalSummaryDto
import com.example.turnosapp.search.data.remote.dto.SyncStatusDto
import com.example.turnosapp.search.domain.model.SearchFilters
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** Llamadas de la búsqueda. Son del catálogo, que responde con su copia local (Constitución P-06). */
class SearchApi(
    private val client: HttpClient,
    private val urls: ServiceUrls,
) {
    suspend fun getCategories(): List<CategoryDto> =
        client.get("${urls.catalog}/api/professional-categories").body()

    /**
     * Arma los parámetros del contrato con los filtros que haya. Un filtro vacío no se envía, y
     * `enabled` va siempre para que se vea qué se pidió. Las fechas y horas van en formato ISO
     * (`2026-11-26`, `18:00`), que es lo que espera el catálogo. El orden queda el del catálogo:
     * apellido y nombre.
     *
     * @param page la página, desde 0.
     */
    suspend fun searchProfessionals(filters: SearchFilters, page: Int, size: Int): PagedResponse<ProfessionalSummaryDto> {
        val response = client.get("${urls.catalog}/api/professionals") {
            filters.name.trim().takeIf { it.isNotEmpty() }?.let { parameter("name", it) }
            filters.category?.let { parameter("categoryId", it.id) }
            parameter("enabled", filters.enabled)
            filters.date?.let { parameter("date", it.toString()) }
            filters.timeRange?.let {
                parameter("timeFrom", it.from.toString())
                parameter("timeTo", it.to.toString())
            }
            parameter("page", page)
            parameter("size", size)
        }
        return response.toPagedResponse(page, size)
    }

    /** Responde siempre, aunque el catálogo no esté listo: es donde se ve su estado. */
    suspend fun getSyncStatus(): SyncStatusDto =
        client.get("${urls.catalog}/api/catalog/sync-status").body()
}
