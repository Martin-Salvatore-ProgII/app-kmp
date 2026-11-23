package com.example.turnosapp.admin.ui

import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.admin.domain.model.AdminReservationFilters
import kotlinx.datetime.LocalDate

/** Todo lo que dibuja Admin (HU-10). La paginación es la misma que en Mis reservas. */
data class AdminUiState(
    /** Lo que está escrito en el campo Usuario; se aplica recién al buscar (ADR-0082). */
    val ownerInput: String = "",
    /** Los filtros con los que se pidió la lista. */
    val filters: AdminReservationFilters = AdminReservationFilters(),
    /** La hoja o el calendario abierto, o null si no hay ninguno. */
    val openPicker: AdminPicker? = null,
    val reservations: List<AdminReservation> = emptyList(),
    /** El total que informa turnos para estos filtros, aunque no se hayan cargado todas. */
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

    val hasFilters: Boolean get() = filters != AdminReservationFilters()

    // Desde y Hasta van incluidos y Desde no puede ser posterior a Hasta: turnos lo rechazaría con un
    // 400. El calendario no deja elegir esos días, así ese error nunca aparece.
    fun canPickFrom(date: LocalDate): Boolean = filters.to == null || date <= filters.to

    fun canPickTo(date: LocalDate): Boolean = filters.from == null || date >= filters.from
}

/** Qué elige el administrador fuera de la pantalla: el estado en una hoja, o una fecha en el calendario. */
enum class AdminPicker { STATUS, FROM, TO }
