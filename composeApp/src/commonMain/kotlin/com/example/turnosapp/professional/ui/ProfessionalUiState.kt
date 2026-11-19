package com.example.turnosapp.professional.ui

import com.example.turnosapp.professional.domain.model.ProfessionalAgenda
import kotlinx.datetime.LocalDate

/**
 * @param upcomingDays los accesos directos a los turnos; el primero es el día buscado, si lo hubo.
 * @param searchedDate la fecha del filtro de Buscar, para marcar su tarjeta.
 * @param isPickingDate si está abierto el calendario de "Elegir otra fecha".
 */
data class ProfessionalUiState(
    val today: LocalDate,
    val searchedDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val agenda: ProfessionalAgenda? = null,
    val upcomingDays: List<LocalDate> = emptyList(),
    val errorMessage: String? = null,
    val isPickingDate: Boolean = false,
)
