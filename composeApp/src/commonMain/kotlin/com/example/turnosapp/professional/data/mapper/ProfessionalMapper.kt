package com.example.turnosapp.professional.data.mapper

import com.example.turnosapp.professional.data.remote.dto.AgendaDto
import com.example.turnosapp.professional.data.remote.dto.WeeklyScheduleDto
import com.example.turnosapp.professional.domain.model.ProfessionalAgenda
import com.example.turnosapp.professional.domain.model.WeeklySchedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/** Pasa la agenda del catálogo al modelo de la pantalla. Un día o una hora inválidos hacen fallar la lectura. */
class ProfessionalMapper {

    fun toDomain(dto: AgendaDto): ProfessionalAgenda =
        ProfessionalAgenda(
            id = dto.id,
            firstName = dto.firstName,
            lastName = dto.lastName,
            categoryName = dto.category.name,
            enabled = dto.enabled,
            schedules = dto.weeklySchedules.map(::toDomain),
        )

    // LocalTime.parse acepta HH:mm y HH:mm:ss, como pide la REF §4 a los clientes.
    private fun toDomain(dto: WeeklyScheduleDto): WeeklySchedule =
        WeeklySchedule(
            dayOfWeek = DayOfWeek.valueOf(dto.dayOfWeek),
            startTime = LocalTime.parse(dto.startTime),
            endTime = LocalTime.parse(dto.endTime),
        )
}
