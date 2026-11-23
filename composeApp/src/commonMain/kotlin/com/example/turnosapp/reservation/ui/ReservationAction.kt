package com.example.turnosapp.reservation.ui

import kotlinx.datetime.LocalDate

sealed interface ReservationAction {
    /** La pantalla se ve: empieza (o sigue) la consulta del estado. */
    data object ScreenStarted : ReservationAction

    /** La pantalla dejó de verse (la app pasó a segundo plano): se pausa la consulta. */
    data object ScreenStopped : ReservationAction

    data class PhoneChanged(val value: String) : ReservationAction
    data object SubmitPhoneClicked : ReservationAction
    data object BackClicked : ReservationAction
    data object MyReservationsClicked : ReservationAction
    data object SearchClicked : ReservationAction
    data object ChooseAnotherSlotClicked : ReservationAction
}

sealed interface ReservationEffect {
    data object NavigateBack : ReservationEffect
    data object NavigateToMyReservations : ReservationEffect
    data object NavigateToSearch : ReservationEffect
    data class NavigateToAvailability(val professionalId: Long, val date: LocalDate) : ReservationEffect
}
