package com.example.turnosapp.search.ui

import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.ProfessionalSummary
import com.example.turnosapp.search.domain.model.SearchFilters
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Todo lo que dibuja Buscar. Qué hoja o selector está abierto también vive acá, así la pantalla
 * es una función pura del estado y el ViewModel decide cuándo se abre y se cierra cada uno.
 *
 * @param today el día de hoy, para que el calendario no deje elegir días pasados (ADR-0079).
 */
data class SearchUiState(
    val today: LocalDate,
    val filters: SearchFilters = SearchFilters(),
    val categories: List<Category> = emptyList(),
    val professionals: List<ProfessionalSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val openSheet: SearchSheet? = null,
    val customRange: CustomRangeDraft = CustomRangeDraft(),
)

/** Las hojas inferiores y el calendario que abren los chips de los filtros. */
enum class SearchSheet { CATEGORY, DATE, TIME_RANGE, ENABLED }

/**
 * La franja personalizada mientras se arma, antes de aplicarla.
 *
 * @param expanded si se ven los campos Desde y Hasta.
 * @param editing qué hora se está eligiendo con el reloj, o null si no hay reloj abierto.
 */
data class CustomRangeDraft(
    val expanded: Boolean = false,
    val from: LocalTime? = null,
    val to: LocalTime? = null,
    val editing: TimeField? = null,
    val errorMessage: String? = null,
)

enum class TimeField { FROM, TO }
