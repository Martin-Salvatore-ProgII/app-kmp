package com.example.turnosapp.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.admin.data.AdminReservationRepository
import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.error.toUserMessage
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
 * Detalle admin de cualquier reserva y su cancelación en nombre del dueño (HU-10, ADR-0082). No consulta
 * solo: se actualiza con el botón Actualizar.
 *
 * @param clock el reloj del teléfono, para saber si el turno ya empezó; en las pruebas, uno fijo.
 */
class AdminReservationDetailViewModel(
    private val repository: AdminReservationRepository,
    private val reservationId: String,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminReservationDetailUiState())
    val state: StateFlow<AdminReservationDetailUiState> = _state.asStateFlow()

    private val _effects = Channel<AdminReservationDetailEffect>(Channel.BUFFERED)
    val effects: Flow<AdminReservationDetailEffect> = _effects.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: AdminReservationDetailAction) {
        when (action) {
            AdminReservationDetailAction.Refresh -> load()
            AdminReservationDetailAction.BackClicked ->
                viewModelScope.launch { _effects.send(AdminReservationDetailEffect.NavigateBack) }
            AdminReservationDetailAction.CancelClicked ->
                if (_state.value.canCancel) _state.update { it.copy(isCancelDialogOpen = true, cancelReason = "", notice = null) }
            is AdminReservationDetailAction.CancelReasonChanged ->
                // El campo no deja pasar del máximo: así nunca se llega al 400 de turnos por el largo.
                _state.update { it.copy(cancelReason = action.value.take(ADMIN_CANCEL_REASON_MAX_LENGTH)) }
            AdminReservationDetailAction.CancelDismissed ->
                if (!_state.value.isCancelling) _state.update { it.copy(isCancelDialogOpen = false) }
            AdminReservationDetailAction.CancelConfirmed -> cancel()
        }
    }

    // Mientras recarga, la reserva anterior queda a la vista; solo se pierde si la nueva no se pudo leer.
    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.get(reservationId)
                .onSuccess { reservation -> _state.update { it.copy(isLoading = false).showing(reservation) } }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, reservation = null, canCancel = false, errorMessage = error.toUserMessage()) }
                }
        }
    }

    private fun cancel() {
        val current = _state.value
        if (!current.canConfirmCancel || !current.canCancel) return
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

    private fun AdminReservationDetailUiState.showing(reservation: AdminReservation) =
        copy(reservation = reservation, canCancel = reservation.reservation.canBeCancelled(clock.now()))

    private companion object {
        const val NOT_CANCELLABLE = "RESERVATION_NOT_CANCELLABLE"
    }
}
