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

    private fun repositoryAnswering(body: String, totalCount: String? = null): SearchRepository {
        val engine = MockEngine { request ->
            lastRequest = request
            val headers = buildMap {
                put(HttpHeaders.ContentType, listOf("application/json"))
                totalCount?.let { put("X-Total-Count", listOf(it)) }
            }
            respond(body, HttpStatusCode.OK, headersOf(*headers.toList().toTypedArray()))
        }
        val client = createHttpClient(engine, FakeSessionRepository())
        val api = SearchApi(client, ServiceUrls(turnos = "https://turnos", catalog = "https://catalogo"))
        return SearchRepositoryImpl(api, SearchMapper())
    }

    @Test
    fun `sin filtros pide solo los habilitados, la página y su tamaño`() = runTest {
        val repository = repositoryAnswering("[]")

        repository.search(SearchFilters(), page = 2)

        assertEquals("https://catalogo/api/professionals?enabled=true&page=2&size=20", lastRequest!!.url.toString())
    }

    @Test
    fun `el total sale de la cabecera X-Total-Count`() = runTest {
        val repository = repositoryAnswering(PROFESSIONAL_JSON, totalCount = "21")

        val page = repository.search(SearchFilters(), page = 0).getOrThrow()

        assertEquals(21, page.totalCount)
        assertEquals(1, page.items.size)
    }

    @Test
    fun `sin la cabecera, el total es lo que ya llegó`() = runTest {
        val repository = repositoryAnswering(PROFESSIONAL_JSON)

        val page = repository.search(SearchFilters(), page = 1).getOrThrow()

        assertEquals(21, page.totalCount)
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

        repository.search(filters, page = 0)

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
        val repository = repositoryAnswering(PROFESSIONAL_JSON, totalCount = "1")

        val result = repository.search(SearchFilters(), page = 0)

        assertEquals(listOf(ProfessionalSummary(202, "Martin", "Rivas", "Cardiologia", true)), result.getOrThrow().items)
    }

    @Test
    fun `lee las categorías e ignora los campos que no usa`() = runTest {
        val repository = repositoryAnswering("""[{"id":1,"name":"Cardiologia","description":"Corazón"}]""")

        val result = repository.getCategories()

        assertEquals(listOf(Category(1, "Cardiologia")), result.getOrThrow())
        assertEquals("https://catalogo/api/professional-categories", lastRequest!!.url.toString())
    }

    private companion object {
        const val PROFESSIONAL_JSON =
            """[{"id":202,"firstName":"Martin","lastName":"Rivas","category":{"id":1,"name":"Cardiologia"},"enabled":true}]"""
    }
}
