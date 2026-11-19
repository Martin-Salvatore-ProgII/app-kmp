package com.example.turnosapp.availability.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.availability.data.AvailabilityRepository
import com.example.turnosapp.availability.domain.FindAttendedDayUseCase
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.error.toUserMessage
import com.example.turnosapp.professional.data.ProfessionalRepository
import kotlinx.coroutines.Job
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
 * Turnos disponibles (HU-06, ADR-0080): los turnos libres de un profesional en una fecha, con
 * flechas para pasar al día anterior o siguiente en que atiende.
 *
 * Pide dos cosas: la disponibilidad a turnos y la agenda al catálogo. La agenda solo sirve para
 * las flechas y la especialidad; si no llega, las flechas avanzan día por día.
 *
 * @param today hoy en la zona del teléfono (ver [com.example.turnosapp.search.ui.SearchViewModel]).
 */
class AvailabilityViewModel(
    private val availabilityRepository: AvailabilityRepository,
    private val professionalRepository: ProfessionalRepository,
    private val findAttendedDay: FindAttendedDayUseCase,
    private val professionalId: Long,
    date: LocalDate,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
) : ViewModel() {

    private val _state = MutableStateFlow(
        AvailabilityUiState(
            today = today,
            date = date,
            nextDay = findAttendedDay.next(emptySet(), date),
            previousDay = findAttendedDay.previous(emptySet(), date, today),
        ),
    )
    val state: StateFlow<AvailabilityUiState> = _state.asStateFlow()

    private val _effects = Channel<AvailabilityEffect>(Channel.BUFFERED)
    val effects: Flow<AvailabilityEffect> = _effects.receiveAsFlow()

    // La carga en curso. Cambiar de día la cancela: tocar las flechas rápido nunca deja ver los
    // turnos de un día anterior con la fecha nueva.
    private var slotsJob: Job? = null

    init {
        loadAgenda()
        loadSlots()
    }

    fun onAction(action: AvailabilityAction) {
        when (action) {
            AvailabilityAction.PreviousDayClicked -> _state.value.previousDay?.let(::changeDate)
            AvailabilityAction.NextDayClicked -> changeDate(_state.value.nextDay)
            AvailabilityAction.DateClicked -> _state.update { it.copy(isPickingDate = true) }
            is AvailabilityAction.DateChosen -> {
                _state.update { it.copy(isPickingDate = false) }
                changeDate(action.date)
            }
            AvailabilityAction.DatePickerDismissed -> _state.update { it.copy(isPickingDate = false) }
            AvailabilityAction.Retry -> loadSlots()
            AvailabilityAction.BackClicked -> viewModelScope.launch { _effects.send(AvailabilityEffect.NavigateBack) }
        }
    }

    private fun changeDate(date: LocalDate) {
        _state.update { it.withDate(date) }
        loadSlots()
    }

    private fun loadAgenda() {
        viewModelScope.launch {
            // Si falla, no se muestra nada: la disponibilidad informa su propio error.
            professionalRepository.getAgenda(professionalId).onSuccess { agenda ->
                _state.update {
                    it.copy(
                        professionalName = it.professionalName ?: agenda.fullName,
                        categoryName = agenda.categoryName,
                        attendedDays = agenda.attendedDays,
                    ).withDate(it.date)
                }
            }
        }
    }

    private fun loadSlots() {
        slotsJob?.cancel()
        slotsJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, slots = null, errorMessage = null, isDateOutOfRange = false) }
            availabilityRepository.getAvailability(professionalId, _state.value.date)
                .onSuccess { availability ->
                    _state.update {
                        it.copy(isLoading = false, slots = availability.slots, professionalName = availability.professionalFullName)
                    }
                }
                .onFailure { error ->
                    // La fecha fuera del horizonte tiene su propia vista, con el calendario a mano (ADR-0080).
                    val outOfRange = error is AppError.Business && error.code == DATE_OUT_OF_RANGE
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isDateOutOfRange = outOfRange,
                            errorMessage = if (outOfRange) null else error.toUserMessage(),
                        )
                    }
                }
        }
    }

    /** Cambia la fecha y recalcula las flechas con los días de atención que se conozcan. */
    private fun AvailabilityUiState.withDate(date: LocalDate): AvailabilityUiState =
        copy(
            date = date,
            nextDay = findAttendedDay.next(attendedDays, date),
            previousDay = findAttendedDay.previous(attendedDays, date, today),
        )

    private companion object {
        const val DATE_OUT_OF_RANGE = "DATE_OUT_OF_RANGE"
    }
}
