package com.example.turnosapp.myreservations.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.error.toUserMessage
import com.example.turnosapp.reservation.data.ReservationRepository
import com.example.turnosapp.reservation.domain.model.Reservation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Detalle de una reserva propia y su cancelación (HU-08, HU-09, ADR-0081). Turnos responde con su
 * base, así que se ve aunque la cátedra no esté disponible; cancelar sí necesita a la cátedra.
 *
 * @param clock el reloj del teléfono, para saber si el turno ya empezó; en las pruebas, uno fijo.
 */
class ReservationDetailViewModel(
    private val repository: ReservationRepository,
    private val reservationId: String,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationDetailUiState())
    val state: StateFlow<ReservationDetailUiState> = _state.asStateFlow()

    private val _effects = Channel<ReservationDetailEffect>(Channel.BUFFERED)
    val effects: Flow<ReservationDetailEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: ReservationDetailAction) {
        when (action) {
            ReservationDetailAction.Retry -> load()
            ReservationDetailAction.BackClicked -> send(ReservationDetailEffect.NavigateBack)
            ReservationDetailAction.SearchClicked -> send(ReservationDetailEffect.NavigateToSearch)
            ReservationDetailAction.CancelClicked ->
                if (_state.value.canCancel) _state.update { it.copy(isCancelDialogOpen = true, cancelReason = "", notice = null) }
            is ReservationDetailAction.CancelReasonChanged ->
                // El campo no deja pasar del máximo: así nunca se llega al 400 de turnos por el largo.
                _state.update { it.copy(cancelReason = action.value.take(CANCEL_REASON_MAX_LENGTH)) }
            ReservationDetailAction.CancelDismissed ->
                if (!_state.value.isCancelling) _state.update { it.copy(isCancelDialogOpen = false) }
            ReservationDetailAction.CancelConfirmed -> cancel()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.get(reservationId)
                .onSuccess { reservation -> _state.update { it.copy(isLoading = false).showing(reservation) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.toUserMessage()) } }
        }
    }

    private fun cancel() {
        val current = _state.value
        if (current.isCancelling || !current.canCancel) return
        viewModelScope.launch {
            _state.update { it.copy(isCancelling = true) }
            repository.cancel(reservationId, current.cancelReason)
                .onSuccess { cancelled ->
                    _state.update { it.copy(isCancelling = false, isCancelDialogOpen = false).showing(cancelled) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isCancelling = false, isCancelDialogOpen = false, notice = error.toUserMessage()) }
                    // Turnos dijo que ya no se puede (no confirmada o el turno empezó, ADR-0071): se
                    // recarga para mostrar cómo está ahora y esconder el botón si corresponde.
                    if (error is AppError.Business && error.code == NOT_CANCELLABLE) load()
                }
        }
    }

    private fun ReservationDetailUiState.showing(reservation: Reservation) =
        copy(reservation = reservation, canCancel = reservation.canBeCancelled(clock.now()))

    private fun send(effect: ReservationDetailEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private companion object {
        const val NOT_CANCELLABLE = "RESERVATION_NOT_CANCELLABLE"
    }
}
