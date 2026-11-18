package com.example.turnosapp.search.ui

import com.example.turnosapp.search.data.SearchRepository
import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.ProfessionalSummary
import com.example.turnosapp.search.domain.model.SearchFilters

/** Repositorio de búsqueda para las pruebas: responde lo que el escenario pide y anota cada búsqueda. */
class FakeSearchRepository(
    var categoriesResult: Result<List<Category>> = Result.success(listOf(CARDIOLOGY)),
    var searchResult: Result<List<ProfessionalSummary>> = Result.success(listOf(RIVAS)),
) : SearchRepository {

    /** Los filtros de cada búsqueda, en orden. */
    val searches = mutableListOf<SearchFilters>()

    override suspend fun getCategories(): Result<List<Category>> = categoriesResult

    override suspend fun search(filters: SearchFilters): Result<List<ProfessionalSummary>> {
        searches += filters
        return searchResult
    }

    companion object {
        val CARDIOLOGY = Category(1, "Cardiologia")
        val RIVAS = ProfessionalSummary(202, "Martin", "Rivas", "Cardiologia", enabled = true)
    }
}
