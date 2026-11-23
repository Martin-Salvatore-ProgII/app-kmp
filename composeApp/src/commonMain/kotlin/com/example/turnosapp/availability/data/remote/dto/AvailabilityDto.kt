package com.example.turnosapp.availability.data.remote.dto

import kotlinx.serialization.Serializable

// La respuesta de `GET /api/availability` (`docs/arq/contratos.md`). Solo los campos que usa la app:
// la versión del catálogo no se muestra. Las horas llegan como `10:00:00`.

@Serializable
data class AvailabilityDto(
    val professionalId: Long,
    val professionalFirstName: String,
    val professionalLastName: String,
    val date: String,
    val slots: List<SlotDto>,
)

@Serializable
data class SlotDto(
    val startTime: String,
    val endTime: String,
)
