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

    private fun repositoryAnswering(
        status: HttpStatusCode,
        body: String,
        contentType: String = "application/json",
        totalCount: Int? = null,
    ): ReservationRepository {
        val engine = MockEngine { request ->
            lastRequest = request
            val headers = buildMap {
                put(HttpHeaders.ContentType, listOf(contentType))
                totalCount?.let { put("X-Total-Count", listOf(it.toString())) }
            }
            respond(body, status, headersOf(*headers.toList().toTypedArray()))
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

    @Test
    fun `el listado pide el estado, la página y su tamaño, y lee el total de la cabecera`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, "[${reservationJson("CONFIRMED")}]", totalCount = 21)

        val page = repository.list(ReservationStatus.CONFIRMED, page = 1).getOrThrow()

        assertEquals("https://turnos/api/reservations?status=CONFIRMED&page=1&size=20", lastRequest!!.url.toString())
        assertEquals(21, page.totalCount)
        assertEquals(ReservationStatus.CONFIRMED, page.items.single().status)
    }

    @Test
    fun `sin filtro no se envía el estado`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, "[]", totalCount = 0)

        val page = repository.list(status = null, page = 0).getOrThrow()

        assertEquals("https://turnos/api/reservations?page=0&size=20", lastRequest!!.url.toString())
        assertEquals(0, page.totalCount)
    }

    @Test
    fun `lee cuándo se creó, cuándo se canceló y el motivo`() = runTest {
        val cancelled = reservationJson("CANCELLED", ""","cancelledAt":"2026-11-23T19:40:00Z","cancellationReason":"No llego"""")
        val repository = repositoryAnswering(HttpStatusCode.OK, "[$cancelled]", totalCount = 1)

        val reservation = repository.list(ReservationStatus.CANCELLED, page = 0).getOrThrow().items.single()

        assertEquals(Instant.parse("2026-11-19T14:35:00Z"), reservation.createdAt)
        assertEquals(Instant.parse("2026-11-23T19:40:00Z"), reservation.cancelledAt)
        assertEquals("No llego", reservation.cancellationReason)
    }

    @Test
    fun `cancelar envía el motivo sin espacios de más y lee la reserva cancelada`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, reservationJson("CANCELLED"))

        val reservation = repository.cancel("5b1f", "  No llego por trabajo. ").getOrThrow()

        assertEquals(HttpMethod.Post, lastRequest!!.method)
        assertEquals("https://turnos/api/reservations/5b1f/cancel", lastRequest!!.url.toString())
        assertEquals("""{"reason":"No llego por trabajo."}""", (lastRequest!!.body as TextContent).text)
        assertEquals(ReservationStatus.CANCELLED, reservation.status)
    }

    @Test
    fun `un motivo en blanco se envía como cancelación sin motivo`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, reservationJson("CANCELLED"))

        repository.cancel("5b1f", "   ").getOrThrow()

        assertEquals("""{"reason":null}""", (lastRequest!!.body as TextContent).text)
    }

    @Test
    fun `una reserva que no se puede cancelar llega con su code`() = runTest {
        val repository = repositoryAnswering(
            HttpStatusCode.Conflict,
            """{"status":409,"code":"RESERVATION_NOT_CANCELLABLE"}""",
            "application/problem+json",
        )

        assertEquals(AppError.Business(409, "RESERVATION_NOT_CANCELLABLE"), repository.cancel("5b1f", "").exceptionOrNull())
    }
}
