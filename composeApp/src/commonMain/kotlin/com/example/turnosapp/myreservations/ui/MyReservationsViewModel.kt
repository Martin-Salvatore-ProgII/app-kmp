package com.example.turnosapp.myreservations.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.core.error.toUserMessage
import com.example.turnosapp.reservation.data.ReservationRepository
import com.example.turnosapp.reservation.domain.model.Reservation
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mis reservas (HU-08, ADR-0081): las reservas propias, de la más nueva a la más vieja, con un filtro
 * por estado y "Cargar más". No carga en `init`: carga cada vez que la pantalla se muestra, así al
 * volver de una cancelación o de una reserva terminada la lista ya está al día.
 */
class MyReservationsViewModel(
    private val repository: ReservationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyReservationsUiState())
    val state: StateFlow<MyReservationsUiState> = _state.asStateFlow()

    private val _effects = Channel<MyReservationsEffect>(Channel.BUFFERED)
    val effects: Flow<MyReservationsEffect> = _effects.receiveAsFlow()

    // La carga en curso. Una nueva la cancela, así la respuesta de un filtro viejo nunca pisa a la
    // del filtro nuevo (el mismo criterio que en Buscar).
    private var loadJob: Job? = null

    fun onAction(action: MyReservationsAction) {
        when (action) {
            MyReservationsAction.ScreenStarted, MyReservationsAction.Retry -> loadFirstPage()
            is MyReservationsAction.FilterSelected -> {
                if (action.filter == _state.value.filter) return
                _state.update { it.copy(filter = action.filter) }
                loadFirstPage()
            }
            MyReservationsAction.LoadMore -> loadMore()
            is MyReservationsAction.ReservationClicked -> openReservation(action.reservation)
            MyReservationsAction.SearchClicked -> send(MyReservationsEffect.NavigateToSearch)
        }
    }

    // Carga desde la primera página. La lista anterior queda a la vista mientras llega la nueva,
    // para que al volver a la pantalla no parpadee.
    private fun loadFirstPage() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isLoadingMore = false, errorMessage = null, loadMoreErrorMessage = null) }
            repository.list(_state.value.filter.status, page = 0)
                .onSuccess { page ->
                    _state.update {
                        it.copy(isLoading = false, reservations = page.items, totalCount = page.totalCount, loadedPages = 1)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            reservations = emptyList(),
                            totalCount = 0,
                            loadedPages = 0,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    // Suma la página siguiente. Usa el mismo Job: si cambia el filtro mientras llega, se cancela.
    private fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.canLoadMore) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreErrorMessage = null) }
            repository.list(current.filter.status, page = current.loadedPages)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoadingMore = false,
                            reservations = it.reservations + page.items,
                            totalCount = page.totalCount,
                            loadedPages = it.loadedPages + 1,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoadingMore = false, loadMoreErrorMessage = error.toUserMessage()) }
                }
        }
    }

    private fun openReservation(reservation: Reservation) {
        send(MyReservationsEffect.NavigateToReservation(reservation.id))
    }

    private fun send(effect: MyReservationsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
