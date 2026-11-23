package com.example.turnosapp.myreservations.ui

import com.example.turnosapp.reservation.domain.model.Reservation

/** Todo lo que dibuja Mis reservas (HU-08). La paginación es la misma que en Buscar. */
data class MyReservationsUiState(
    val filter: ReservationFilter = ReservationFilter.ALL,
    val reservations: List<Reservation> = emptyList(),
    /** El total que informa turnos para este filtro, aunque no se hayan cargado todas. */
    val totalCount: Int = 0,
    /** Cuántas páginas hay cargadas; la próxima a pedir es esta. */
    val loadedPages: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** Falló la primera página: no hay lista que mostrar. */
    val errorMessage: String? = null,
    /** Falló una página siguiente: la lista que ya estaba queda a la vista. */
    val loadMoreErrorMessage: String? = null,
) {
    val canLoadMore: Boolean get() = reservations.size < totalCount

    /** Ya se cargó al menos una vez y no hay nada: se muestra el mensaje de lista vacía. */
    val isEmpty: Boolean get() = !isLoading && errorMessage == null && loadedPages > 0 && reservations.isEmpty()
}
