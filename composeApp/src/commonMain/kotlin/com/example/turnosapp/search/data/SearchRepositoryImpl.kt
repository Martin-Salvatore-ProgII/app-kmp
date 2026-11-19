package com.example.turnosapp.search.data

import com.example.turnosapp.core.network.runCatchingApi
import com.example.turnosapp.search.data.mapper.SearchMapper
import com.example.turnosapp.search.data.remote.SearchApi
import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.Page
import com.example.turnosapp.search.domain.model.ProfessionalSummary
import com.example.turnosapp.search.domain.model.SearchFilters

/**
 * Cuántos profesionales se piden por página. Es una elección de la app, no un valor del catálogo
 * (que acepta hasta 100): se manda siempre, así no depende del tamaño por defecto del backend.
 */
internal const val PAGE_SIZE = 20

class SearchRepositoryImpl(
    private val api: SearchApi,
    private val mapper: SearchMapper,
) : SearchRepository {

    override suspend fun getCategories(): Result<List<Category>> =
        runCatchingApi { api.getCategories().map(mapper::toDomain) }

    override suspend fun search(filters: SearchFilters, page: Int): Result<Page<ProfessionalSummary>> =
        runCatchingApi {
            val response = api.searchProfessionals(filters, page, PAGE_SIZE)
            Page(response.items.map(mapper::toDomain), response.totalCount)
        }

    override suspend fun isCatalogSyncing(): Result<Boolean> =
        runCatchingApi { api.getSyncStatus().syncInProgress }
}
