package com.example.turnosapp.admin.data

import com.example.turnosapp.admin.data.mapper.AdminReservationMapper
import com.example.turnosapp.admin.data.remote.AdminReservationApi
import com.example.turnosapp.admin.domain.model.AdminReservationFilters
import com.example.turnosapp.admin.domain.model.ReservationOwner
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.createHttpClient
import com.example.turnosapp.core.session.FakeSessionRepository
import com.example.turnosapp.reservation.data.mapper.ReservationMapper
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** El repositorio con el cliente HTTP real contra un servicio de turnos simulado: el contrato administrativo. */
class AdminReservationRepositoryImplTest {

    private var lastRequest: HttpRequestData? = null

    private fun repositoryAnswering(
        status: HttpStatusCode,
        body: String,
        contentType: String = "application/json",
        totalCount: Int? = null,
    ): AdminReservationRepository {
        val engine = MockEngine { request ->
            lastRequest = request
            val headers = buildMap {
                put(HttpHeaders.ContentType, listOf(contentType))
                totalCount?.let { put("X-Total-Count", listOf(it.toString())) }
            }
            respond(body, status, headersOf(*headers.toList().toTypedArray()))
        }
        val api = AdminReservationApi(createHttpClient(engine, FakeSessionRepository()), ServiceUrls("https://turnos", "https://catalogo"))
        return AdminReservationRepositoryImpl(api, AdminReservationMapper(ReservationMapper()))
    }

    // Así llega de turnos: los campos de la reserva al mismo nivel que los de soporte.
    private val cancelledJson =
        """{"id":"5b1f","status":"CANCELLED","professionalId":202,"professionalFirstName":"Martin",
            "professionalLastName":"Rivas","date":"2026-11-26","startTime":"19:30:00","endTime":"20:00:00",
            "expiresAt":null,"message":null,"failureReason":null,"createdAt":"2026-11-19T14:35:00Z",
            "confirmedAt":"2026-11-19T14:36:00Z","cancelledAt":"2026-11-20T10:00:00Z","cancellationReason":"Pidió la baja",
            "owner":{"login":"jperez","firstName":"Juan","lastName":"Pérez"},
            "reservationProcessId":"8c2f6a1e","cancelledBy":"admin"}"""

    @Test
    fun `listar envía los filtros elegidos y lee la reserva con sus datos de soporte`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, "[$cancelledJson]", totalCount = 21)
        val filters = AdminReservationFilters(
            ownerLogin = "jperez",
            status = ReservationStatus.CANCELLED,
            from = LocalDate(2026, 11, 1),
            to = LocalDate(2026, 11, 30),
        )

        val page = repository.list(filters, page = 1).getOrThrow()

        assertEquals(HttpMethod.Get, lastRequest!!.method)
        assertEquals(
            "https://turnos/api/admin/reservations?ownerLogin=jperez&status=CANCELLED&from=2026-11-01&to=2026-11-30&page=1&size=20",
            lastRequest!!.url.toString(),
        )
        assertEquals(21, page.totalCount)
        val reservation = page.items.single()
        assertEquals(ReservationStatus.CANCELLED, reservation.reservation.status)
        assertEquals("Pidió la baja", reservation.reservation.cancellationReason)
        assertEquals(ReservationOwner("jperez", "Juan", "Pérez"), reservation.owner)
        assertEquals("8c2f6a1e", reservation.reservationProcessId)
        assertEquals("admin", reservation.cancelledBy)
    }

    @Test
    fun `sin filtros solo se envía la página`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, "[]", totalCount = 0)

        repository.list(AdminReservationFilters(), page = 0).getOrThrow()

        assertEquals("https://turnos/api/admin/reservations?page=0&size=20", lastRequest!!.url.toString())
    }

    @Test
    fun `una reserva cuyo hold no llegó a crearse no tiene proceso en la cátedra`() = runTest {
        val failed = cancelledJson
            .replace("\"CANCELLED\"", "\"FAILED\"")
            .replace("\"reservationProcessId\":\"8c2f6a1e\"", "\"reservationProcessId\":null")
            .replace("\"cancelledBy\":\"admin\"", "\"cancelledBy\":null")
        val repository = repositoryAnswering(HttpStatusCode.OK, "[$failed]", totalCount = 1)

        val reservation = repository.list(AdminReservationFilters(), page = 0).getOrThrow().items.single()

        assertNull(reservation.reservationProcessId)
        assertNull(reservation.cancelledBy)
    }

    @Test
    fun `un usuario sin ROLE_ADMIN recibe Forbidden`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.Forbidden, """{"status":403}""", "application/problem+json")

        assertEquals(AppError.Forbidden, repository.list(AdminReservationFilters(), page = 0).exceptionOrNull())
    }

    @Test
    fun `ver una reserva pide la ruta administrativa con su id`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, cancelledJson)

        val reservation = repository.get("5b1f").getOrThrow()

        assertEquals(HttpMethod.Get, lastRequest!!.method)
        assertEquals("https://turnos/api/admin/reservations/5b1f", lastRequest!!.url.toString())
        assertEquals("jperez", reservation.owner.login)
    }

    @Test
    fun `cancelar envía el motivo sin los espacios de los extremos`() = runTest {
        val repository = repositoryAnswering(HttpStatusCode.OK, cancelledJson)

        val reservation = repository.cancel("5b1f", "  Pidió la baja  ").getOrThrow()

        assertEquals(HttpMethod.Post, lastRequest!!.method)
        assertEquals("https://turnos/api/admin/reservations/5b1f/cancel", lastRequest!!.url.toString())
        assertEquals("""{"reason":"Pidió la baja"}""", (lastRequest!!.body as TextContent).text)
        assertEquals("admin", reservation.cancelledBy)
    }

    @Test
    fun `cancelar una reserva que ya no se puede llega con su code`() = runTest {
        val repository = repositoryAnswering(
            HttpStatusCode.Conflict,
            """{"status":409,"code":"RESERVATION_NOT_CANCELLABLE"}""",
            "application/problem+json",
        )

        assertEquals(
            AppError.Business(409, "RESERVATION_NOT_CANCELLABLE"),
            repository.cancel("5b1f", "Pidió la baja").exceptionOrNull(),
        )
    }
}
