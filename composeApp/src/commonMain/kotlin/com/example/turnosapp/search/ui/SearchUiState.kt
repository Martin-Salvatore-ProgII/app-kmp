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
    /** El total que informa el catálogo para estos filtros, aunque no se hayan cargado todos. */
    val totalCount: Int = 0,
    /** Cuántas páginas hay cargadas; la próxima a pedir es esta. */
    val loadedPages: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    /** El catálogo está aplicando una sincronización: se busca sobre la copia anterior (ADR-0010). */
    val catalogSyncing: Boolean = false,
    /** El catálogo todavía no tiene su primera copia (`503 CATALOG_NOT_READY`). */
    val catalogNotReady: Boolean = false,
    /** Falló una página siguiente: la lista que ya estaba queda a la vista. */
    val loadMoreErrorMessage: String? = null,
    val openSheet: SearchSheet? = null,
    val customRange: CustomRangeDraft = CustomRangeDraft(),
) {
    val canLoadMore: Boolean get() = professionals.size < totalCount
}

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
