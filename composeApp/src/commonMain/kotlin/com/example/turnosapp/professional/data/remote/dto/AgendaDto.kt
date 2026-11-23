package com.example.turnosapp.professional.data.remote.dto

import kotlinx.serialization.Serializable

// `GET /api/professionals/{id}/agenda` (`docs/arq/contratos.md`). Solo los campos que usa la app:
// la duración de los turnos y la versión del catálogo no se muestran (ADR-0079).

@Serializable
data class AgendaDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val category: AgendaCategoryDto,
    val enabled: Boolean,
    val weeklySchedules: List<WeeklyScheduleDto>,
)

@Serializable
data class AgendaCategoryDto(
    val id: Long,
    val name: String,
)

/** Las horas llegan como `09:00:00` y el día como `MONDAY`. */
@Serializable
data class WeeklyScheduleDto(
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
)
