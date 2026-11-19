package com.example.turnosapp.availability.data

import com.example.turnosapp.availability.data.mapper.AvailabilityMapper
import com.example.turnosapp.availability.data.remote.AvailabilityApi
import com.example.turnosapp.availability.domain.model.Slot
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.createHttpClient
import com.example.turnosapp.core.session.FakeSessionRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** El repositorio con el cliente HTTP real contra un servicio de turnos simulado: el contrato de la disponibilidad. */
class AvailabilityRepositoryImplTest {

    private var lastRequest: HttpRequestData? = null

    private fun repositoryAnswering(status: HttpStatusCode, body: String, contentType: String = "application/json"): AvailabilityRepository {
        val engine = MockEngine { request ->
            lastRequest = request
            respond(body, status, headersOf(HttpHeaders.ContentType, contentType))
        }
        val api = AvailabilityApi(createHttpClient(engine, FakeSessionRepository()), ServiceUrls("https://turnos", "https://catalogo"))
        return AvailabilityRepositoryImpl(api, AvailabilityMapper())
    }

    @Test
    fun `pide la disponibilidad a turnos y lee los turnos`() = runTest {
        val repository = repositoryAnswering(
            HttpStatusCode.OK,
            """
            {"professionalId":202,"professionalFirstName":"Martin","professionalLastName":"Rivas","date":"2026-11-26",
             "catalogVersion":7,"slots":[{"startTime":"18:00:00","endTime":"18:30:00"},{"startTime":"18:30","endTime":"19:00"}]}
            """.trimIndent(),
        )

        val availability = repository.getAvailability(202, LocalDate(2026, 11, 26)).getOrThrow()

        assertEquals("https://turnos/api/availability?professionalId=202&date=2026-11-26", lastRequest!!.url.toString())
        assertEquals("Martin Rivas", availability.professionalFullName)
        assertEquals(
            listOf(Slot(LocalTime(18, 0), LocalTime(18, 30)), Slot(LocalTime(18, 30), LocalTime(19, 0))),
            availability.slots,
        )
        assertEquals(30, availability.slots.first().durationMinutes)
    }

    @Test
    fun `una fecha fuera del horizonte llega con su code`() = runTest {
        val repository = repositoryAnswering(
            HttpStatusCode.BadRequest,
            """{"status":400,"code":"DATE_OUT_OF_RANGE","detail":"x"}""",
            "application/problem+json",
        )

        val error = repository.getAvailability(202, LocalDate(2027, 1, 28)).exceptionOrNull()

        assertEquals(AppError.Business(400, "DATE_OUT_OF_RANGE"), error)
    }
}
