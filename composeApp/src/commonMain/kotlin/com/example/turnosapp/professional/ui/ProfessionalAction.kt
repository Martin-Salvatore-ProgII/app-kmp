package com.example.turnosapp.professional.ui

import kotlinx.datetime.LocalDate

sealed interface ProfessionalAction {
    /** Una tarjeta de los próximos días, o una fecha elegida en el calendario. */
    data class DayChosen(val date: LocalDate) : ProfessionalAction
    data object OtherDateClicked : ProfessionalAction
    data object DatePickerDismissed : ProfessionalAction
    data object Retry : ProfessionalAction
    data object BackClicked : ProfessionalAction
}

sealed interface ProfessionalEffect {
    data class NavigateToAvailability(val professionalId: Long, val date: LocalDate) : ProfessionalEffect
    data object NavigateBack : ProfessionalEffect
}
