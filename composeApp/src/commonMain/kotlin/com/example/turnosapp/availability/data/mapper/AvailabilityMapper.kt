package com.example.turnosapp.availability.data.mapper

import com.example.turnosapp.availability.data.remote.dto.AvailabilityDto
import com.example.turnosapp.availability.data.remote.dto.SlotDto
import com.example.turnosapp.availability.domain.model.Availability
import com.example.turnosapp.availability.domain.model.Slot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Pasa la disponibilidad de turnos al modelo de la pantalla. Una fecha u hora inválidas hacen fallar la lectura. */
class AvailabilityMapper {

    fun toDomain(dto: AvailabilityDto): Availability =
        Availability(
            professionalId = dto.professionalId,
            professionalFirstName = dto.professionalFirstName,
            professionalLastName = dto.professionalLastName,
            date = LocalDate.parse(dto.date),
            slots = dto.slots.map(::toDomain),
        )

    // LocalTime.parse acepta HH:mm y HH:mm:ss, como pide la REF §4 a los clientes.
    private fun toDomain(dto: SlotDto): Slot =
        Slot(startTime = LocalTime.parse(dto.startTime), endTime = LocalTime.parse(dto.endTime))
}
