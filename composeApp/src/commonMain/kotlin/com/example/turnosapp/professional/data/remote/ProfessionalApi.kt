package com.example.turnosapp.professional.data.remote

import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.professional.data.remote.dto.AgendaDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/** La agenda vigente de un profesional, del catálogo (ADR-0017). La misma operación que usa turnos. */
class ProfessionalApi(
    private val client: HttpClient,
    private val urls: ServiceUrls,
) {
    suspend fun getAgenda(professionalId: Long): AgendaDto =
        client.get("${urls.catalog}/api/professionals/$professionalId/agenda").body()
}
