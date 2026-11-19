package com.example.turnosapp.availability.ui

import com.example.turnosapp.availability.domain.model.Slot
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * @param professionalName de la disponibilidad o, mientras no llega, de la agenda.
 * @param attendedDays los días en que atiende, de la agenda; vacío si no se pudo leer.
 * @param previousDay el día anterior en que atiende, o null si sería un día pasado.
 * @param nextDay el próximo día en que atiende. Sin límite: el horizonte lo controla turnos (P-21).
 * @param slots los turnos libres de [date], o null mientras no se cargaron.
 * @param isDateOutOfRange si turnos rechazó la fecha por estar fuera del horizonte.
 * @param isPickingDate si está abierto el calendario.
 */
data class AvailabilityUiState(
    val today: LocalDate,
    val date: LocalDate,
    val nextDay: LocalDate,
    val previousDay: LocalDate? = null,
    val professionalName: String? = null,
    val categoryName: String? = null,
    val attendedDays: Set<DayOfWeek> = emptySet(),
    val isLoading: Boolean = false,
    val slots: List<Slot>? = null,
    val errorMessage: String? = null,
    val isDateOutOfRange: Boolean = false,
    val isPickingDate: Boolean = false,
)
