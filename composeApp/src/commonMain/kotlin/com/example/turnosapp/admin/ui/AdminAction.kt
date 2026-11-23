package com.example.turnosapp.admin.ui

import com.example.turnosapp.reservation.domain.model.ReservationStatus
import kotlinx.datetime.LocalDate

sealed interface AdminAction {
    /** La pantalla se volvió a ver: se recarga la lista con los filtros elegidos. */
    data object ScreenStarted : AdminAction

    data class OwnerChanged(val value: String) : AdminAction

    /** Buscar desde el teclado o la lupa: aplica el usuario escrito. */
    data object OwnerSubmitted : AdminAction
    data object OwnerCleared : AdminAction

    data class PickerOpened(val picker: AdminPicker) : AdminAction
    data object PickerDismissed : AdminAction

    /** null quita el filtro. */
    data class StatusSelected(val status: ReservationStatus?) : AdminAction

    /** null quita el filtro. */
    data class FromSelected(val date: LocalDate?) : AdminAction

    /** null quita el filtro. */
    data class ToSelected(val date: LocalDate?) : AdminAction

    data object Retry : AdminAction
    data object LoadMore : AdminAction

    /** Toda reserva abre el detalle admin, también una en curso (ADR-0082). */
    data class ReservationClicked(val reservationId: String) : AdminAction
}

sealed interface AdminEffect {
    data class NavigateToDetail(val reservationId: String) : AdminEffect
}
