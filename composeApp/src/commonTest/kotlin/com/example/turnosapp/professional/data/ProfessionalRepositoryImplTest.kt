package com.example.turnosapp.professional.data

import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.createHttpClient
import com.example.turnosapp.core.session.FakeSessionRepository
import com.example.turnosapp.professional.data.mapper.ProfessionalMapper
import com.example.turnosapp.professional.data.remote.ProfessionalApi
import com.example.turnosapp.professional.domain.model.WeeklySchedule
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** El repositorio con el cliente HTTP real contra un catálogo simulado: el contrato de la agenda (ADR-0017). */
class ProfessionalRepositoryImplTest {

    private var lastRequest: HttpRequestData? = null

    private fun repositoryAnswering(body: String): ProfessionalRepository {
        val engine = MockEngine { request ->
            lastRequest = request
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = ProfessionalApi(createHttpClient(engine, FakeSessionRepository()), ServiceUrls("https://turnos", "https://catalogo"))
        return ProfessionalRepositoryImpl(api, ProfessionalMapper())
    }

    @Test
    fun `lee la agenda con horas con y sin segundos`() = runTest {
        val repository = repositoryAnswering(
            """
            {"id":202,"firstName":"Martin","lastName":"Rivas","category":{"id":1,"name":"Cardiologia"},"enabled":true,
             "weeklySchedules":[
               {"id":21,"dayOfWeek":"TUESDAY","startTime":"11:30:00","endTime":"14:30:00","slotDurationMinutes":30},
               {"id":22,"dayOfWeek":"THURSDAY","startTime":"18:00","endTime":"21:00","slotDurationMinutes":30}],
             "catalogVersion":7}
            """.trimIndent(),
        )

        val agenda = repository.getAgenda(202).getOrThrow()

        assertEquals("https://catalogo/api/professionals/202/agenda", lastRequest!!.url.toString())
        assertEquals("Martin Rivas", agenda.fullName)
        assertEquals(
            listOf(
                WeeklySchedule(DayOfWeek.TUESDAY, LocalTime(11, 30), LocalTime(14, 30)),
                WeeklySchedule(DayOfWeek.THURSDAY, LocalTime(18, 0), LocalTime(21, 0)),
            ),
            agenda.schedules,
        )
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), agenda.attendedDays)
    }
}
