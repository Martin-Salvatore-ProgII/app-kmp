package com.example.turnosapp.reservation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.error.reservationFailureMessage
import com.example.turnosapp.core.error.toUserMessage
import com.example.turnosapp.reservation.data.ReservationRepository
import com.example.turnosapp.reservation.domain.model.Reservation
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Cada cuánto se consulta el estado del proceso (ADR-0021, RNF-09). Es un valor de la app. */
internal const val POLL_INTERVAL_MILLIS = 2_000L

private const val TICK_MILLIS = 1_000L

/**
 * Reserva en curso (HU-07, ADR-0021, ADR-0080): sigue el proceso consultando su estado mientras no
 * sea final, y lleva la cuenta regresiva hasta `expiresAt`.
 *
 * Dos tareas corren mientras la pantalla se ve, y las dos terminan solas al llegar a un estado final:
 * - la consulta, cada [POLL_INTERVAL_MILLIS];
 * - el reloj, cada segundo, que recalcula el tiempo que queda. Al llegar a cero bloquea el envío del
 *   teléfono, pero no decide el resultado: lo informa turnos en la consulta siguiente.
 *
 * @param clock el reloj del teléfono; en las pruebas, uno que se mueve a mano.
 */
class ReservationViewModel(
    private val repository: ReservationRepository,
    private val reservationId: String,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationUiState())
    val state: StateFlow<ReservationUiState> = _state.asStateFlow()

    private val _effects = Channel<ReservationEffect>(Channel.BUFFERED)
    val effects: Flow<ReservationEffect> = _effects.receiveAsFlow()

    private var pollingJob: Job? = null
    private var tickJob: Job? = null

    fun onAction(action: ReservationAction) {
        when (action) {
            ReservationAction.ScreenStarted -> startFollowing()
            ReservationAction.ScreenStopped -> stopFollowing()
            ReservationAction.BackClicked -> send(ReservationEffect.NavigateBack)
            ReservationAction.MyReservationsClicked -> send(ReservationEffect.NavigateToMyReservations)
            ReservationAction.SearchClicked -> send(ReservationEffect.NavigateToSearch)
            ReservationAction.ChooseAnotherSlotClicked -> _state.value.reservation?.let {
                send(ReservationEffect.NavigateToAvailability(it.professionalId, it.date))
            }
        }
    }

    /** Al volver a la pantalla consulta en el momento, sin esperar el intervalo. */
    private fun startFollowing() {
        if (_state.value.phase.isFinal || pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (refresh()) delay(POLL_INTERVAL_MILLIS)
        }
        tickJob = viewModelScope.launch {
            // Si la consulta se cortó (un proceso que no existe), el reloj no tiene nada que contar.
            while (pollingJob?.isActive == true && !_state.value.phase.isFinal) {
                _state.update { it.withTime() }
                delay(TICK_MILLIS)
            }
        }
    }

    private fun stopFollowing() {
        pollingJob?.cancel()
        tickJob?.cancel()
    }

    /** Lee el proceso. Devuelve si hay que seguir consultando. */
    private suspend fun refresh(): Boolean {
        val result = repository.get(reservationId)
        result.onSuccess { reservation ->
            _state.update { it.copy(reservation = reservation, isConnectionLost = false, errorMessage = null).withTime() }
        }
        result.onFailure { error ->
            when (error) {
                // Sin red o sin servicio no se corta el seguimiento: se avisa y se sigue intentando.
                AppError.Network, AppError.Unavailable, AppError.Unknown -> _state.update { it.copy(isConnectionLost = true) }
                // Un proceso que no existe (404) no va a aparecer: se deja de consultar. El 401 lo resuelve la sesión.
                else -> {
                    _state.update { it.copy(errorMessage = error.toUserMessage()) }
                    return false
                }
            }
        }
        return !_state.value.phase.isFinal
    }

    /** Recalcula el tiempo que queda y, con él, qué se muestra. */
    private fun ReservationUiState.withTime(): ReservationUiState {
        val reservation = reservation ?: return this
        val remaining = reservation.expiresAt
            ?.takeUnless { reservation.status.isFinal }
            ?.let { (it - clock.now()).inWholeSeconds.coerceAtLeast(0) }
        return copy(
            remainingSeconds = remaining,
            totalSeconds = totalSeconds ?: remaining,
            phase = phaseOf(reservation, remaining),
        )
    }

    private fun send(effect: ReservationEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

/** El estado del proceso y el tiempo que queda, pasados a lo que muestra la pantalla (`maquina-estados.md`). */
internal fun phaseOf(reservation: Reservation, remainingSeconds: Long?): ReservationPhase =
    when (reservation.status) {
        ReservationStatus.STARTED, ReservationStatus.HELD, ReservationStatus.AWAITING_REQUEST -> ReservationPhase.Reserving
        // Con el tiempo en cero no se ofrece el campo, aunque la cátedra todavía no haya informado el vencimiento.
        ReservationStatus.AWAITING_PHONE ->
            if (remainingSeconds == 0L) ReservationPhase.TimeUp else ReservationPhase.AwaitingPhone
        ReservationStatus.PHONE_SUBMITTED -> ReservationPhase.Confirming
        ReservationStatus.CONFIRMED -> ReservationPhase.Confirmed
        ReservationStatus.CANCELLED -> ReservationPhase.Cancelled
        ReservationStatus.EXPIRED -> ReservationPhase.Expired
        ReservationStatus.FAILED, ReservationStatus.INVALID -> ReservationPhase.Failed(reservationFailureMessage(reservation.failureReason))
    }
