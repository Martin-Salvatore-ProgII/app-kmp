package com.example.turnosapp.availability.ui

import kotlinx.datetime.LocalDate

sealed interface AvailabilityAction {
    data object PreviousDayClicked : AvailabilityAction
    data object NextDayClicked : AvailabilityAction
    data object DateClicked : AvailabilityAction
    data class DateChosen(val date: LocalDate) : AvailabilityAction
    data object DatePickerDismissed : AvailabilityAction
    data object Retry : AvailabilityAction
    data object BackClicked : AvailabilityAction
}

sealed interface AvailabilityEffect {
    data object NavigateBack : AvailabilityEffect
}
