package com.example.turnosapp.search.data

import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.ProfessionalSummary
import com.example.turnosapp.search.domain.model.SearchFilters

/** Búsqueda de profesionales en el catálogo (HU-05). */
interface SearchRepository {

    /** Las categorías habilitadas, ordenadas por nombre, para el filtro. */
    suspend fun getCategories(): Result<List<Category>>

    /**
     * Los profesionales que cumplen todos los filtros, ordenados por apellido y nombre.
     * Un filtro inválido llega como `AppError.Business` (`DATE_OUT_OF_RANGE`) o `AppError.Validation`.
     */
    suspend fun search(filters: SearchFilters): Result<List<ProfessionalSummary>>
}
