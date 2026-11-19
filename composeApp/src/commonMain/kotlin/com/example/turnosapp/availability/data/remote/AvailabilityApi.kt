package com.example.turnosapp.availability.data.remote

import com.example.turnosapp.availability.data.remote.dto.AvailabilityDto
import com.example.turnosapp.core.network.ServiceUrls
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDate

/** `GET /api/availability` de turnos (`docs/arq/contratos.md`). La fecha va en ISO (`2026-11-26`). */
class AvailabilityApi(
    private val client: HttpClient,
    private val urls: ServiceUrls,
) {
    suspend fun getAvailability(professionalId: Long, date: LocalDate): AvailabilityDto =
        client.get("${urls.turnos}/api/availability") {
            parameter("professionalId", professionalId)
            parameter("date", date.toString())
        }.body()
}
