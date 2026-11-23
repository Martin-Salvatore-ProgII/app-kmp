package com.example.turnosapp.myreservations.ui

import com.example.turnosapp.reservation.domain.model.Reservation

sealed interface MyReservationsAction {
    /** La pantalla se volvió a ver: se recarga la lista (ADR-0081). */
    data object ScreenStarted : MyReservationsAction
    data class FilterSelected(val filter: ReservationFilter) : MyReservationsAction
    data object Retry : MyReservationsAction
    data object LoadMore : MyReservationsAction
    data class ReservationClicked(val reservation: Reservation) : MyReservationsAction
    data object SearchClicked : MyReservationsAction
}

sealed interface MyReservationsEffect {
    /** Una reserva en curso se sigue en Reserva en curso (ADR-0081). */
    data class NavigateToReservation(val reservationId: String) : MyReservationsEffect

    /** Una reserva terminada se ve en el Detalle, donde se puede cancelar si corresponde. */
    data class NavigateToDetail(val reservationId: String) : MyReservationsEffect
    data object NavigateToSearch : MyReservationsEffect
}
