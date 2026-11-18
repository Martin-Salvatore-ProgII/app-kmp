package com.example.turnosapp.search.data

import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.createHttpClient
import com.example.turnosapp.core.session.FakeSessionRepository
import com.example.turnosapp.search.data.mapper.SearchMapper
import com.example.turnosapp.search.data.remote.SearchApi
import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.DayPeriod
import com.example.turnosapp.search.domain.model.ProfessionalSummary
import com.example.turnosapp.search.domain.model.SearchFilters
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** El repositorio con el cliente HTTP real contra un catálogo simulado: el contrato de la búsqueda (HU-05). */
class SearchRepositoryImplTest {

    private var lastRequest: HttpRequestData? = null

    private fun repositoryAnswering(body: String): SearchRepository {
        val engine = MockEngine { request ->
            lastRequest = request
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createHttpClient(engine, FakeSessionRepository())
        val api = SearchApi(client, ServiceUrls(turnos = "https://turnos", catalog = "https://catalogo"))
        return SearchRepositoryImpl(api, SearchMapper())
    }

    @Test
    fun `sin filtros pide solo los habilitados`() = runTest {
        val repository = repositoryAnswering("[]")

        repository.search(SearchFilters())

        assertEquals("https://catalogo/api/professionals?enabled=true", lastRequest!!.url.toString())
    }

    @Test
    fun `envía cada filtro con el nombre y el formato del contrato`() = runTest {
        val repository = repositoryAnswering("[]")
        val filters = SearchFilters(
            name = " mar ",
            category = Category(1, "Cardiologia"),
            date = LocalDate(2026, 11, 26),
            timeRange = DayPeriod.EVENING.range,
            enabled = false,
        )

        repository.search(filters)

        val parameters = lastRequest!!.url.parameters
        assertEquals("mar", parameters["name"])
        assertEquals("1", parameters["categoryId"])
        assertEquals("false", parameters["enabled"])
        assertEquals("2026-11-26", parameters["date"])
        assertEquals("18:00", parameters["timeFrom"])
        assertEquals("23:59", parameters["timeTo"])
    }

    @Test
    fun `traduce cada profesional al modelo de la pantalla`() = runTest {
        val repository = repositoryAnswering(
            """[{"id":202,"firstName":"Martin","lastName":"Rivas","category":{"id":1,"name":"Cardiologia"},"enabled":true}]""",
        )

        val result = repository.search(SearchFilters())

        assertEquals(listOf(ProfessionalSummary(202, "Martin", "Rivas", "Cardiologia", true)), result.getOrThrow())
    }

    @Test
    fun `lee las categorías e ignora los campos que no usa`() = runTest {
        val repository = repositoryAnswering("""[{"id":1,"name":"Cardiologia","description":"Corazón"}]""")

        val result = repository.getCategories()

        assertEquals(listOf(Category(1, "Cardiologia")), result.getOrThrow())
        assertEquals("https://catalogo/api/professional-categories", lastRequest!!.url.toString())
    }
}
