package com.example.turnosapp.availability.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.availability.data.AvailabilityRepository
import com.example.turnosapp.availability.domain.FindAttendedDayUseCase
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.error.toUserMessage
import com.example.turnosapp.core.error.reservationFailureMessage
import com.example.turnosapp.professional.data.ProfessionalRepository
import com.example.turnosapp.reservation.data.ReservationRepository
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
 * Turnos disponibles (HU-06, HU-07, ADR-0080): los turnos libres de un profesional en una fecha, con
 * flechas para pasar al día anterior o siguiente en que atiende, y el inicio de la reserva.
 *
 * Pide dos cosas: la disponibilidad a turnos y la agenda al catálogo. La agenda solo sirve para
 * las flechas y la especialidad; si no llega, las flechas avanzan día por día.
 *
 * @param today hoy en la zona del teléfono (ver [com.example.turnosapp.search.ui.SearchViewModel]).
 */
class AvailabilityViewModel(
    private val availabilityRepository: AvailabilityRepository,
    private val professionalRepository: ProfessionalRepository,
    private val reservationRepository: ReservationRepository,
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
            is AvailabilityAction.SlotSelected -> _state.update {
                it.copy(selectedSlot = action.slot.takeUnless { slot -> slot == it.selectedSlot }, rejectionMessage = null)
            }
            AvailabilityAction.ReserveClicked -> reserve()
            AvailabilityAction.ViewActiveReservationClicked -> openActiveReservation()
            AvailabilityAction.ActiveReservationDialogDismissed -> _state.update { it.copy(showActiveReservationDialog = false) }
        }
    }

    private fun changeDate(date: LocalDate) {
        _state.update { it.withDate(date).copy(selectedSlot = null, rejectionMessage = null) }
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

    /**
     * Un solo toque crea el hold y lo confirma (ADR-0019). Si el proceso arrancó, se pasa a Reserva en
     * curso; si volvió ya rechazado (ADR-0069), se queda acá con el motivo y los turnos al día (ADR-0080).
     */
    private fun reserve() {
        val current = _state.value
        val slot = current.selectedSlot ?: return
        if (current.isReserving) return // Turnos también lo impide (ADR-0024), pero así no sale un segundo pedido.
        viewModelScope.launch {
            _state.update { it.copy(isReserving = true, rejectionMessage = null) }
            reservationRepository.start(professionalId, current.date, slot.startTime)
                .onSuccess { reservation ->
                    if (reservation.status.isFinal) {
                        _state.update {
                            it.copy(isReserving = false, selectedSlot = null, rejectionMessage = reservationFailureMessage(reservation.failureReason))
                        }
                        loadSlots()
                    } else {
                        _state.update { it.copy(isReserving = false) }
                        _effects.send(AvailabilityEffect.NavigateToReservation(reservation.id))
                    }
                }
                .onFailure { error ->
                    if (error is AppError.Business && error.code == ACTIVE_RESERVATION_EXISTS) {
                        _state.update { it.copy(isReserving = false, showActiveReservationDialog = true) }
                    } else {
                        // Un rechazo del turno (INVALID_SLOT, por ejemplo) deja la lista vieja: se recarga.
                        _state.update { it.copy(isReserving = false, selectedSlot = null, rejectionMessage = error.toUserMessage()) }
                        if (error is AppError.Business) loadSlots()
                    }
                }
        }
    }

    private fun openActiveReservation() {
        viewModelScope.launch {
            reservationRepository.findActive()
                .onSuccess { active ->
                    _state.update { it.copy(showActiveReservationDialog = false) }
                    // Si terminó entre el rechazo y el toque, ya no hay a dónde ir: se puede reservar de nuevo.
                    active?.let { _effects.send(AvailabilityEffect.NavigateToReservation(it.id)) }
                }
                .onFailure { error ->
                    _state.update { it.copy(showActiveReservationDialog = false, rejectionMessage = error.toUserMessage()) }
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
        const val ACTIVE_RESERVATION_EXISTS = "ACTIVE_RESERVATION_EXISTS"
    }
}
