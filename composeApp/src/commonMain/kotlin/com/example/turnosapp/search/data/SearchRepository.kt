package com.example.turnosapp.search.data

import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.Page
import com.example.turnosapp.search.domain.model.ProfessionalSummary
import com.example.turnosapp.search.domain.model.SearchFilters

/** Búsqueda de profesionales en el catálogo (HU-05). */
interface SearchRepository {

    /** Las categorías habilitadas, ordenadas por nombre, para el filtro. */
    suspend fun getCategories(): Result<List<Category>>

    /**
     * Una página de los profesionales que cumplen todos los filtros, ordenados por apellido y nombre,
     * con el total para saber si hay más.
     * Un filtro inválido llega como `AppError.Business` (`DATE_OUT_OF_RANGE`) o `AppError.Validation`.
     *
     * @param page la página, desde 0.
     */
    suspend fun search(filters: SearchFilters, page: Int): Result<Page<ProfessionalSummary>>

    /** Si el catálogo está aplicando una sincronización; mientras tanto se busca sobre la copia anterior (ADR-0010). */
    suspend fun isCatalogSyncing(): Result<Boolean>
}
