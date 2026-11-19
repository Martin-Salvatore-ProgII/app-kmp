package com.example.turnosapp.professional.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * La agenda vigente de un profesional (ADR-0017): sus datos y los horarios semanales habilitados,
 * ordenados de lunes a domingo.
 *
 * @param enabled el habilitado efectivo (ADR-0014). Un profesional no habilitado se muestra igual,
 * pero sin accesos a sus turnos.
 */
data class ProfessionalAgenda(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val categoryName: String,
    val enabled: Boolean,
    val schedules: List<WeeklySchedule>,
) {
    val fullName: String get() = "$firstName $lastName"

    /** Los días de la semana en que atiende, para saber qué fechas ofrecer. */
    val attendedDays: Set<DayOfWeek> get() = schedules.map { it.dayOfWeek }.toSet()
}

/** Un horario semanal: un día y su franja de atención, en hora local del negocio (REF §4). */
data class WeeklySchedule(
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
)
