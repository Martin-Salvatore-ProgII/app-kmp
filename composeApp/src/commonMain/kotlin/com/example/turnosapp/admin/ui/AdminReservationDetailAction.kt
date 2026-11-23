package com.example.turnosapp.admin.ui

sealed interface AdminReservationDetailAction {
    /** Actualizar o Reintentar: vuelve a pedir la reserva (ADR-0082). */
    data object Refresh : AdminReservationDetailAction
    data object BackClicked : AdminReservationDetailAction
    data object CancelClicked : AdminReservationDetailAction
    data class CancelReasonChanged(val value: String) : AdminReservationDetailAction
    data object CancelDismissed : AdminReservationDetailAction
    data object CancelConfirmed : AdminReservationDetailAction
}

sealed interface AdminReservationDetailEffect {
    data object NavigateBack : AdminReservationDetailEffect
}
