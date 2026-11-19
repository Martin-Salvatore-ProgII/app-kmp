package com.example.turnosapp.availability.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Los turnos libres de un profesional en una fecha (HU-06). Una lista vacía es una respuesta
 * válida: no atiende ese día o ya no quedan libres.
 */
data class Availability(
    val professionalId: Long,
    val professionalFirstName: String,
    val professionalLastName: String,
    val date: LocalDate,
    val slots: List<Slot>,
) {
    val professionalFullName: String get() = "$professionalFirstName $professionalLastName"
}

/** Un turno libre, en hora local del negocio (ADR-0016). */
data class Slot(
    val startTime: LocalTime,
    val endTime: LocalTime,
) {
    /** La duración en minutos, para mostrar "30 min" junto a la cantidad de turnos. */
    val durationMinutes: Int get() = (endTime.toSecondOfDay() - startTime.toSecondOfDay()) / 60
}
