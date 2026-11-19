package com.example.turnosapp.professional.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.core.error.toUserMessage
import com.example.turnosapp.professional.data.ProfessionalRepository
import com.example.turnosapp.professional.domain.CalculateUpcomingDaysUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Profesional (HU-05, ADR-0017, ADR-0079): la agenda vigente y los accesos directos a sus turnos.
 *
 * @param searchedDate la fecha con la que se buscó en Buscar, o null si no se filtró por fecha.
 * @param today hoy en la zona del teléfono (ver [com.example.turnosapp.search.ui.SearchViewModel]).
 */
class ProfessionalViewModel(
    private val repository: ProfessionalRepository,
    private val calculateUpcomingDays: CalculateUpcomingDaysUseCase,
    private val professionalId: Long,
    searchedDate: LocalDate?,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
) : ViewModel() {

    private val _state = MutableStateFlow(ProfessionalUiState(today = today, searchedDate = searchedDate))
    val state: StateFlow<ProfessionalUiState> = _state.asStateFlow()

    private val _effects = Channel<ProfessionalEffect>(Channel.BUFFERED)
    val effects: Flow<ProfessionalEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: ProfessionalAction) {
        when (action) {
            is ProfessionalAction.DayChosen -> {
                _state.update { it.copy(isPickingDate = false) }
                // Solo se ofrecen días de un profesional habilitado, pero se valida igual.
                if (_state.value.agenda?.enabled == true) {
                    viewModelScope.launch {
                        _effects.send(ProfessionalEffect.NavigateToAvailability(professionalId, action.date))
                    }
                }
            }
            ProfessionalAction.OtherDateClicked -> _state.update { it.copy(isPickingDate = true) }
            ProfessionalAction.DatePickerDismissed -> _state.update { it.copy(isPickingDate = false) }
            ProfessionalAction.Retry -> load()
            ProfessionalAction.BackClicked -> viewModelScope.launch { _effects.send(ProfessionalEffect.NavigateBack) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getAgenda(professionalId)
                .onSuccess { agenda ->
                    val current = _state.value
                    // Un profesional no habilitado no tiene accesos a turnos (ADR-0014).
                    val days = if (agenda.enabled) {
                        calculateUpcomingDays(agenda.attendedDays, current.today, current.searchedDate)
                    } else {
                        emptyList()
                    }
                    _state.update { it.copy(isLoading = false, agenda = agenda, upcomingDays = days) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, errorMessage = error.toUserMessage()) }
                }
        }
    }
}
