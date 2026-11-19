package com.example.turnosapp.search.ui

import com.example.turnosapp.search.data.SearchRepository
import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.Page
import com.example.turnosapp.search.domain.model.ProfessionalSummary
import com.example.turnosapp.search.domain.model.SearchFilters

/**
 * Repositorio de búsqueda para las pruebas: responde lo que el escenario pide y anota cada búsqueda.
 * [searchResult] responde la primera página, y [nextPageResult] las siguientes.
 */
class FakeSearchRepository(
    var categoriesResult: Result<List<Category>> = Result.success(listOf(CARDIOLOGY)),
    var searchResult: Result<Page<ProfessionalSummary>> = Result.success(Page(listOf(RIVAS), totalCount = 1)),
    var nextPageResult: Result<Page<ProfessionalSummary>> = Result.success(Page(emptyList(), totalCount = 1)),
) : SearchRepository {

    /** Los filtros de cada búsqueda, en orden. */
    val searches = mutableListOf<SearchFilters>()

    /** La página pedida en cada búsqueda, en orden. */
    val pages = mutableListOf<Int>()

    override suspend fun getCategories(): Result<List<Category>> = categoriesResult

    override suspend fun search(filters: SearchFilters, page: Int): Result<Page<ProfessionalSummary>> {
        searches += filters
        pages += page
        return if (page == 0) searchResult else nextPageResult
    }

    companion object {
        val CARDIOLOGY = Category(1, "Cardiologia")
        val RIVAS = ProfessionalSummary(202, "Martin", "Rivas", "Cardiologia", enabled = true)
        val TORRES = ProfessionalSummary(201, "Ana", "Torres", "Cardiologia", enabled = true)
    }
}
