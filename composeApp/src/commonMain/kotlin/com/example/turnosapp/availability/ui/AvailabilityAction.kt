package com.example.turnosapp.availability.ui

import com.example.turnosapp.availability.domain.model.Slot
import kotlinx.datetime.LocalDate

sealed interface AvailabilityAction {
    data object PreviousDayClicked : AvailabilityAction
    data object NextDayClicked : AvailabilityAction
    data object DateClicked : AvailabilityAction
    data class DateChosen(val date: LocalDate) : AvailabilityAction
    data object DatePickerDismissed : AvailabilityAction
    data object Retry : AvailabilityAction
    data object BackClicked : AvailabilityAction
    data class SlotSelected(val slot: Slot) : AvailabilityAction
    data object ReserveClicked : AvailabilityAction
    data object ViewActiveReservationClicked : AvailabilityAction
    data object ActiveReservationDialogDismissed : AvailabilityAction
}

sealed interface AvailabilityEffect {
    data object NavigateBack : AvailabilityEffect
    data class NavigateToReservation(val reservationId: String) : AvailabilityEffect
}
