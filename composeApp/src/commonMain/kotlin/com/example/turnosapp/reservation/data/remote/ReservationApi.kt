package com.example.turnosapp.reservation.data.remote

import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.reservation.data.remote.dto.ReservationDto
import com.example.turnosapp.reservation.data.remote.dto.StartReservationRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Las operaciones de reservas de turnos (`docs/arq/contratos.md`, Reservas). */
class ReservationApi(
    private val client: HttpClient,
    private val urls: ServiceUrls,
) {
    suspend fun start(request: StartReservationRequestDto): ReservationDto =
        client.post("${urls.turnos}/api/reservations") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /** La primera página de las reservas propias, con el orden por defecto: las más nuevas primero. */
    suspend fun listLatest(size: Int): List<ReservationDto> =
        client.get("${urls.turnos}/api/reservations") {
            parameter("page", 0)
            parameter("size", size)
        }.body()
}
