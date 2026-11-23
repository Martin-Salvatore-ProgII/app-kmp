package com.example.turnosapp.reservation.data.remote

import com.example.turnosapp.core.network.PagedResponse
import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.toPagedResponse
import com.example.turnosapp.reservation.data.remote.dto.CancelRequestDto
import com.example.turnosapp.reservation.data.remote.dto.PhoneRequestDto
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

    suspend fun get(reservationId: String): ReservationDto =
        client.get("${urls.turnos}/api/reservations/$reservationId").body()

    suspend fun submitPhone(reservationId: String, request: PhoneRequestDto): ReservationDto =
        client.post("${urls.turnos}/api/reservations/$reservationId/phone") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun cancel(reservationId: String, request: CancelRequestDto): ReservationDto =
        client.post("${urls.turnos}/api/reservations/$reservationId/cancel") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    /**
     * Una página de las reservas propias, con el orden por defecto de turnos: las más nuevas primero.
     *
     * @param status un estado de la máquina, o null para todas. Turnos acepta uno solo (ADR-0081).
     * @param page la página, desde 0.
     */
    suspend fun list(status: String?, page: Int, size: Int): PagedResponse<ReservationDto> =
        client.get("${urls.turnos}/api/reservations") {
            status?.let { parameter("status", it) }
            parameter("page", page)
            parameter("size", size)
        }.toPagedResponse(page, size)
}
