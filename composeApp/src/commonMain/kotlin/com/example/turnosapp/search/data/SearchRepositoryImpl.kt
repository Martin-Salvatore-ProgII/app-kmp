package com.example.turnosapp.search.data

import com.example.turnosapp.core.network.runCatchingApi
import com.example.turnosapp.search.data.mapper.SearchMapper
import com.example.turnosapp.search.data.remote.SearchApi
import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.ProfessionalSummary
import com.example.turnosapp.search.domain.model.SearchFilters

class SearchRepositoryImpl(
    private val api: SearchApi,
    private val mapper: SearchMapper,
) : SearchRepository {

    override suspend fun getCategories(): Result<List<Category>> =
        runCatchingApi { api.getCategories().map(mapper::toDomain) }

    override suspend fun search(filters: SearchFilters): Result<List<ProfessionalSummary>> =
        runCatchingApi { api.searchProfessionals(filters).map(mapper::toDomain) }
}
