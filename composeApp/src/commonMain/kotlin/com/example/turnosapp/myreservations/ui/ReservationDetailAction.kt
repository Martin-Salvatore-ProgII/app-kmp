package com.example.turnosapp.myreservations.ui

sealed interface ReservationDetailAction {
    data object Retry : ReservationDetailAction
    data object BackClicked : ReservationDetailAction
    data object CancelClicked : ReservationDetailAction
    data class CancelReasonChanged(val value: String) : ReservationDetailAction
    data object CancelDismissed : ReservationDetailAction
    data object CancelConfirmed : ReservationDetailAction
    data object SearchClicked : ReservationDetailAction
}

sealed interface ReservationDetailEffect {
    data object NavigateBack : ReservationDetailEffect
    data object NavigateToSearch : ReservationDetailEffect
}
