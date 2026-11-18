package com.example.turnosapp.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.core.error.toUserMessage
import com.example.turnosapp.search.data.SearchRepository
import com.example.turnosapp.search.domain.model.SearchFilters
import com.example.turnosapp.search.domain.model.TimeRange
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Cuánto se espera después de la última letra del nombre antes de buscar (ADR-0079). */
internal const val NAME_DEBOUNCE_MILLIS = 300L

/**
 * Búsqueda de profesionales (HU-05, ADR-0079). No hay botón Buscar: al entrar se busca sin
 * filtros, y cada cambio de un filtro vuelve a buscar.
 *
 * @param today hoy en la zona del teléfono. La app es para usuarios en Argentina, la misma zona de
 * la agenda (ADR-0016); si difiere cerca de la medianoche, el catálogo rechaza la fecha y se avisa.
 */
class SearchViewModel(
    private val repository: SearchRepository,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState(today = today))
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    // La búsqueda en curso, o la que espera a que se deje de escribir. Una búsqueda nueva la
    // cancela, así una respuesta vieja nunca pisa a una más nueva.
    private var searchJob: Job? = null

    init {
        loadCategories()
        search()
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.NameChanged -> changeFilters(delayMillis = NAME_DEBOUNCE_MILLIS) { it.copy(name = action.value) }
            SearchAction.NameCleared -> changeFilters { it.copy(name = "") }

            is SearchAction.SheetOpened -> openSheet(action.sheet)
            SearchAction.SheetDismissed -> closeSheet()

            is SearchAction.CategorySelected -> {
                closeSheet()
                changeFilters { it.copy(category = action.category) }
            }
            is SearchAction.DateSelected -> {
                closeSheet()
                // Sin fecha, la franja no es válida (ADR-0065): se va con ella.
                changeFilters { it.copy(date = action.date, timeRange = if (action.date == null) null else it.timeRange) }
            }
            is SearchAction.DayPeriodSelected -> {
                closeSheet()
                changeFilters { it.copy(timeRange = action.period?.range) }
            }

            SearchAction.CustomRangeExpanded -> expandCustomRange()
            is SearchAction.CustomTimeEditing -> updateDraft { it.copy(editing = action.field) }
            is SearchAction.CustomTimePicked -> updateDraft {
                when (it.editing) {
                    TimeField.FROM -> it.copy(from = action.time, editing = null, errorMessage = null)
                    TimeField.TO -> it.copy(to = action.time, editing = null, errorMessage = null)
                    null -> it
                }
            }
            SearchAction.CustomRangeApplied -> applyCustomRange()

            is SearchAction.EnabledSelected -> {
                closeSheet()
                changeFilters { it.copy(enabled = action.enabled) }
            }

            SearchAction.Retry -> search()
        }
    }

    private fun changeFilters(delayMillis: Long = 0, change: (SearchFilters) -> SearchFilters) {
        val before = _state.value.filters
        _state.update { it.copy(filters = change(it.filters)) }
        if (_state.value.filters != before) search(delayMillis)
    }

    private fun search(delayMillis: Long = 0) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(delayMillis)
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.search(_state.value.filters)
                .onSuccess { professionals ->
                    _state.update { it.copy(isLoading = false, professionals = professionals) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, professionals = emptyList(), errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    // Si las categorías no se pudieron cargar, el chip sigue ahí y se reintenta al abrir su hoja.
    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().onSuccess { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    private fun openSheet(sheet: SearchSheet) {
        val current = _state.value
        // La franja necesita una fecha: su chip solo aparece con una, pero se valida igual.
        if (sheet == SearchSheet.TIME_RANGE && current.filters.date == null) return
        if (sheet == SearchSheet.CATEGORY && current.categories.isEmpty()) loadCategories()
        _state.update { it.copy(openSheet = sheet, customRange = CustomRangeDraft()) }
    }

    private fun closeSheet() {
        _state.update { it.copy(openSheet = null, customRange = CustomRangeDraft()) }
    }

    private fun updateDraft(change: (CustomRangeDraft) -> CustomRangeDraft) {
        _state.update { it.copy(customRange = change(it.customRange)) }
    }

    // Abre los campos con la franja que ya hay, para corregirla en lugar de empezar de cero.
    private fun expandCustomRange() {
        val current = _state.value.filters.timeRange
        updateDraft { CustomRangeDraft(expanded = true, from = current?.from, to = current?.to) }
    }

    // La app valida lo mismo que el catálogo (ADR-0065) para avisar antes de buscar; la
    // autoridad sigue siendo el catálogo, que responde 400 si algo no cumple.
    private fun applyCustomRange() {
        val draft = _state.value.customRange
        val from = draft.from
        val to = draft.to
        when {
            from == null || to == null -> updateDraft { it.copy(errorMessage = "Elegí las dos horas.") }
            from >= to -> updateDraft { it.copy(errorMessage = "La hora de inicio tiene que ser anterior a la de fin.") }
            else -> {
                closeSheet()
                changeFilters { it.copy(timeRange = TimeRange(from, to)) }
            }
        }
    }
}
