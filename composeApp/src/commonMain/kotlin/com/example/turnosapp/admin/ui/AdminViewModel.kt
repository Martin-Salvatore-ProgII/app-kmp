package com.example.turnosapp.admin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.admin.data.AdminReservationRepository
import com.example.turnosapp.admin.domain.model.AdminReservationFilters
import com.example.turnosapp.core.error.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Admin (HU-10, ADR-0082): todas las reservas, de la más nueva a la más vieja, con filtros que se
 * suman y "Cargar más". Igual que Mis reservas, carga cada vez que la pantalla se muestra.
 */
class AdminViewModel(
    private val repository: AdminReservationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    // La carga en curso. Una nueva la cancela, así la respuesta de un filtro viejo nunca pisa a la
    // del filtro nuevo (el mismo criterio que en Buscar y Mis reservas).
    private var loadJob: Job? = null

    fun onAction(action: AdminAction) {
        when (action) {
            AdminAction.ScreenStarted, AdminAction.Retry -> loadFirstPage()
            is AdminAction.OwnerChanged -> _state.update { it.copy(ownerInput = action.value.take(OWNER_MAX_LENGTH)) }
            AdminAction.OwnerSubmitted -> applyFilters { it.copy(ownerLogin = _state.value.ownerInput.trim().ifEmpty { null }) }
            AdminAction.OwnerCleared -> {
                _state.update { it.copy(ownerInput = "") }
                applyFilters { it.copy(ownerLogin = null) }
            }
            is AdminAction.PickerOpened -> _state.update { it.copy(openPicker = action.picker) }
            AdminAction.PickerDismissed -> _state.update { it.copy(openPicker = null) }
            is AdminAction.StatusSelected -> applyFilters { it.copy(status = action.status) }
            is AdminAction.FromSelected -> applyFilters { it.copy(from = action.date) }
            is AdminAction.ToSelected -> applyFilters { it.copy(to = action.date) }
            AdminAction.LoadMore -> loadMore()
        }
    }

    // Cierra la hoja o el calendario y, si los filtros cambiaron, vuelve a pedir desde la primera página.
    private fun applyFilters(change: (AdminReservationFilters) -> AdminReservationFilters) {
        val current = _state.value.filters
        val updated = change(current)
        _state.update { it.copy(filters = updated, openPicker = null) }
        if (updated != current) loadFirstPage()
    }

    // La lista anterior queda a la vista mientras llega la nueva, para que no parpadee.
    private fun loadFirstPage() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isLoadingMore = false, errorMessage = null, loadMoreErrorMessage = null) }
            repository.list(_state.value.filters, page = 0)
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

    // Suma la página siguiente. Usa el mismo Job: si cambian los filtros mientras llega, se cancela.
    private fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.canLoadMore) return
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true, loadMoreErrorMessage = null) }
            repository.list(current.filters, page = current.loadedPages)
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

    private companion object {
        /** El largo máximo de un login (HU-01); turnos rechaza uno más largo. */
        const val OWNER_MAX_LENGTH = 50
    }
}
