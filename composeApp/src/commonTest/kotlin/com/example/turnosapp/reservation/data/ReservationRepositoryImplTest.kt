package com.example.turnosapp.reservation.data

import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.createHttpClient
import com.example.turnosapp.core.session.FakeSessionRepository
import com.example.turnosapp.reservation.data.mapper.ReservationMapper
import com.example.turnosapp.reservation.data.remote.ReservationApi
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** El repositorio con el cliente HTTP real contra un servicio de turnos simulado: el contrato de las reservas. */
class ReservationRepositoryImplTest {

    private var lastRequest: HttpRequestData? = null

    private fun repositoryAnswering(status: HttpStatusCode, body: String, contentType: String = "application/json"): ReservationRepository {
        val engine = MockEngine { request ->
            lastRequest = request
            respond(body, status, headersOf(HttpHeaders.ContentType, contentType))
        }
        val api = ReservationApi(createHttpClient(engine, FakeSessionRepository()), ServiceUrls("https://turnos", "https://catalogo"))
        return ReservationRepositoryImpl(api, ReservationMapper())
    }

    private fun reservationJson(status: String, extra: String = "") =
        """{"id":"5b1f","status":"$status","professionalId":202,"professionalFirstName":"Martin",
            "professionalLastName":"Rivas","date":"2026-11-26","startTime":"19:30:00","endTime":"20:00:00",
            "expiresAt":"2026-11-19T14:44:50Z","message":null,"failureReason":null,"createdAt":"2026-11-19T14:35:00Z"$extra}"""

    @Test
    fun `iniciar envía el profesional, la fecha y la hora, y lee el proceso`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.Created, reservationJson("AWAITING_REQUEST"))

        val reservation = repository.start(202, LocalDate(2026, 11, 26), LocalTime(19, 30)).getOrThrow()

        assertEquals(HttpMethod.Post, lastRequest!!.method)
        assertEquals("https://turnos/api/reservations", lastRequest!!.url.toString())
        assertEquals(
            """{"professionalId":202,"date":"2026-11-26","startTime":"19:30"}""",
            (lastRequest!!.body as TextContent).text,
        )
        assertEquals(ReservationStatus.AWAITING_REQUEST, reservation.status)
        assertEquals(LocalTime(19, 30), reservation.startTime)
        assertEquals(Instant.parse("2026-11-19T14:44:50Z"), reservation.expiresAt)
    }

    @Test
    fun `una reserva en curso llega con su code`() = runTest {
        val repository = repositoryAnswering(
            HttpStatusCode.Conflict,
            """{"status":409,"code":"ACTIVE_RESERVATION_EXISTS"}""",
            "application/problem+json",
        )

        val error = repository.start(202, LocalDate(2026, 11, 26), LocalTime(19, 30)).exceptionOrNull()

        assertEquals(AppError.Business(409, "ACTIVE_RESERVATION_EXISTS"), error)
    }

    @Test
    fun `el proceso activo es la última reserva si no es final`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, "[${reservationJson("AWAITING_PHONE")}]")

        val active = repository.findActive().getOrThrow()

        assertEquals("https://turnos/api/reservations?page=0&size=1", lastRequest!!.url.toString())
        assertEquals(ReservationStatus.AWAITING_PHONE, active?.status)
    }

    @Test
    fun `sin reservas o con la última terminada no hay proceso activo`() = runTest {
        assertNull(repositoryAnswering(HttpStatusCode.OK, "[]").findActive().getOrThrow())
        assertNull(repositoryAnswering(HttpStatusCode.OK, "[${reservationJson("CONFIRMED")}]").findActive().getOrThrow())
    }

    @Test
    fun `enviar el teléfono lo manda tal como se escribió`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.Accepted, reservationJson("PHONE_SUBMITTED"))

        val reservation = repository.submitPhone("5b1f", "+54 9 11 5555-1234").getOrThrow()

        assertEquals("https://turnos/api/reservations/5b1f/phone", lastRequest!!.url.toString())
        assertEquals("""{"phoneNumber":"+54 9 11 5555-1234"}""", (lastRequest!!.body as TextContent).text)
        assertEquals(ReservationStatus.PHONE_SUBMITTED, reservation.status)
    }
}
