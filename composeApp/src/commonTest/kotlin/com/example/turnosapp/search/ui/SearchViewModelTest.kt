package com.example.turnosapp.search.ui

import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.model.Page
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import com.example.turnosapp.reservation.ui.FakeReservationRepository
import com.example.turnosapp.reservation.ui.reservation
import com.example.turnosapp.search.domain.model.DayPeriod
import com.example.turnosapp.search.domain.model.SearchFilters
import com.example.turnosapp.search.domain.model.TimeRange
import com.example.turnosapp.search.ui.FakeSearchRepository.Companion.CARDIOLOGY
import com.example.turnosapp.search.ui.FakeSearchRepository.Companion.RIVAS
import com.example.turnosapp.search.ui.FakeSearchRepository.Companion.TORRES
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// runTest usa el reloj virtual del dispatcher de prueba que reemplaza a Main: la espera del nombre
// se avanza con advanceTimeBy, sin esperar de verdad.
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val repository = FakeSearchRepository()
    private val today = LocalDate(2026, 11, 18)
    private val thursday = LocalDate(2026, 11, 26)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val reservations = FakeReservationRepository()

    private fun newViewModel() = SearchViewModel(repository, reservations, today)

    @Test
    fun `al entrar busca sin filtros y carga las categorías`() = runTest {
        val viewModel = newViewModel()

        assertEquals(listOf(SearchFilters()), repository.searches)
        assertEquals(listOf(RIVAS), viewModel.state.value.professionals)
        assertEquals(listOf(CARDIOLOGY), viewModel.state.value.categories)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `el nombre busca una sola vez, cuando se deja de escribir`() = runTest {
        val viewModel = newViewModel()
        repository.searches.clear()

        viewModel.onAction(SearchAction.NameChanged("m"))
        viewModel.onAction(SearchAction.NameChanged("ma"))
        viewModel.onAction(SearchAction.NameChanged("mar"))
        advanceTimeBy(NAME_DEBOUNCE_MILLIS - 1)
        runCurrent()
        assertEquals(emptyList(), repository.searches)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("mar"), repository.searches.map { it.name })
    }

    @Test
    fun `elegir una categoría busca al instante y cierra la hoja`() = runTest {
        val viewModel = newViewModel()
        viewModel.onAction(SearchAction.SheetOpened(SearchSheet.CATEGORY))

        viewModel.onAction(SearchAction.CategorySelected(CARDIOLOGY))

        assertEquals(CARDIOLOGY, repository.searches.last().category)
        assertNull(viewModel.state.value.openSheet)
    }

    @Test
    fun `elegir lo mismo que ya estaba no vuelve a buscar`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(SearchAction.EnabledSelected(true))

        assertEquals(1, repository.searches.size)
    }

    @Test
    fun `la franja no se abre sin fecha`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(SearchAction.SheetOpened(SearchSheet.TIME_RANGE))

        assertNull(viewModel.state.value.openSheet)
    }

    @Test
    fun `quitar la fecha quita también la franja`() = runTest {
        val viewModel = newViewModel()
        viewModel.onAction(SearchAction.DateSelected(thursday))
        viewModel.onAction(SearchAction.DayPeriodSelected(DayPeriod.EVENING))
        assertEquals(DayPeriod.EVENING.range, repository.searches.last().timeRange)

        viewModel.onAction(SearchAction.DateSelected(null))

        assertEquals(SearchFilters(), repository.searches.last())
    }

    @Test
    fun `una franja personalizada válida se aplica y busca`() = runTest {
        val viewModel = newViewModel()
        viewModel.onAction(SearchAction.DateSelected(thursday))
        viewModel.onAction(SearchAction.SheetOpened(SearchSheet.TIME_RANGE))
        viewModel.onAction(SearchAction.CustomRangeExpanded)
        pick(viewModel, TimeField.FROM, LocalTime(10, 0))
        pick(viewModel, TimeField.TO, LocalTime(11, 0))

        viewModel.onAction(SearchAction.CustomRangeApplied)

        assertEquals(TimeRange(LocalTime(10, 0), LocalTime(11, 0)), repository.searches.last().timeRange)
        assertNull(viewModel.state.value.openSheet)
    }

    @Test
    fun `una franja personalizada con el inicio después del fin no se aplica`() = runTest {
        val viewModel = newViewModel()
        viewModel.onAction(SearchAction.DateSelected(thursday))
        viewModel.onAction(SearchAction.SheetOpened(SearchSheet.TIME_RANGE))
        viewModel.onAction(SearchAction.CustomRangeExpanded)
        pick(viewModel, TimeField.FROM, LocalTime(11, 0))
        pick(viewModel, TimeField.TO, LocalTime(10, 0))
        val searchesBefore = repository.searches.size

        viewModel.onAction(SearchAction.CustomRangeApplied)

        assertEquals(searchesBefore, repository.searches.size)
        assertEquals("La hora de inicio tiene que ser anterior a la de fin.", viewModel.state.value.customRange.errorMessage)
        assertEquals(SearchSheet.TIME_RANGE, viewModel.state.value.openSheet)
    }

    @Test
    fun `una franja personalizada incompleta pide las dos horas`() = runTest {
        val viewModel = newViewModel()
        viewModel.onAction(SearchAction.DateSelected(thursday))
        viewModel.onAction(SearchAction.SheetOpened(SearchSheet.TIME_RANGE))
        viewModel.onAction(SearchAction.CustomRangeExpanded)
        pick(viewModel, TimeField.FROM, LocalTime(10, 0))

        viewModel.onAction(SearchAction.CustomRangeApplied)

        assertEquals("Elegí las dos horas.", viewModel.state.value.customRange.errorMessage)
    }

    @Test
    fun `una fecha fuera del horizonte muestra el mensaje propio del código`() = runTest {
        repository.searchResult = Result.failure(AppError.Business(400, "DATE_OUT_OF_RANGE"))
        val viewModel = newViewModel()

        viewModel.onAction(SearchAction.DateSelected(LocalDate(2027, 6, 1)))

        assertEquals("La fecha elegida está fuera del rango permitido.", viewModel.state.value.errorMessage)
        assertEquals(emptyList(), viewModel.state.value.professionals)
    }

    @Test
    fun `reintentar repite la búsqueda con los mismos filtros`() = runTest {
        repository.searchResult = Result.failure(AppError.Network)
        val viewModel = newViewModel()
        repository.searchResult = Result.success(Page(listOf(RIVAS), totalCount = 1))

        viewModel.onAction(SearchAction.Retry)

        assertNull(viewModel.state.value.errorMessage)
        assertEquals(listOf(RIVAS), viewModel.state.value.professionals)
    }

    @Test
    fun `cargar más suma la página siguiente con los mismos filtros`() = runTest {
        repository.searchResult = Result.success(Page(listOf(RIVAS), totalCount = 2))
        repository.nextPageResult = Result.success(Page(listOf(TORRES), totalCount = 2))
        val viewModel = newViewModel()
        assertTrue(viewModel.state.value.canLoadMore)

        viewModel.onAction(SearchAction.LoadMore)

        assertEquals(listOf(0, 1), repository.pages)
        assertEquals(listOf(RIVAS, TORRES), viewModel.state.value.professionals)
        assertFalse(viewModel.state.value.canLoadMore)
    }

    @Test
    fun `sin más resultados, cargar más no pide nada`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(SearchAction.LoadMore)

        assertEquals(listOf(0), repository.pages)
    }

    @Test
    fun `si falla la página siguiente, la lista que estaba queda a la vista`() = runTest {
        repository.searchResult = Result.success(Page(listOf(RIVAS), totalCount = 2))
        repository.nextPageResult = Result.failure(AppError.Network)
        val viewModel = newViewModel()

        viewModel.onAction(SearchAction.LoadMore)

        assertEquals(listOf(RIVAS), viewModel.state.value.professionals)
        assertEquals(
            "No se pudo conectar con el servidor. Revisá tu conexión y probá de nuevo.",
            viewModel.state.value.loadMoreErrorMessage,
        )
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `cambiar un filtro vuelve a la primera página`() = runTest {
        repository.searchResult = Result.success(Page(listOf(RIVAS), totalCount = 2))
        repository.nextPageResult = Result.success(Page(listOf(TORRES), totalCount = 2))
        val viewModel = newViewModel()
        viewModel.onAction(SearchAction.LoadMore)

        viewModel.onAction(SearchAction.CategorySelected(CARDIOLOGY))

        assertEquals(listOf(0, 1, 0), repository.pages)
        assertEquals(listOf(RIVAS), viewModel.state.value.professionals)
    }

    @Test
    fun `con una sincronización en curso busca igual y muestra el aviso`() = runTest {
        repository.syncingResult = Result.success(true)

        val viewModel = newViewModel()

        assertTrue(viewModel.state.value.catalogSyncing)
        assertEquals(listOf(RIVAS), viewModel.state.value.professionals)
    }

    @Test
    fun `si el estado del catálogo no responde, no hay aviso y la búsqueda sigue`() = runTest {
        repository.syncingResult = Result.failure(AppError.Network)

        val viewModel = newViewModel()

        assertFalse(viewModel.state.value.catalogSyncing)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `sin la primera copia del catálogo se muestra la espera, no un error`() = runTest {
        repository.searchResult = Result.failure(AppError.CatalogNotReady)
        val viewModel = newViewModel()

        assertTrue(viewModel.state.value.catalogNotReady)
        assertNull(viewModel.state.value.errorMessage)

        repository.searchResult = Result.success(Page(listOf(RIVAS), totalCount = 1))
        viewModel.onAction(SearchAction.Retry)

        assertFalse(viewModel.state.value.catalogNotReady)
        assertEquals(listOf(RIVAS), viewModel.state.value.professionals)
    }

    @Test
    fun `tocar un profesional abre su pantalla con la fecha buscada`() = runTest {
        val viewModel = newViewModel()
        viewModel.onAction(SearchAction.DateSelected(thursday))

        viewModel.onAction(SearchAction.ProfessionalClicked(202))

        assertEquals(SearchEffect.NavigateToProfessional(202, thursday), viewModel.effects.first())
    }

    private fun pick(viewModel: SearchViewModel, field: TimeField, time: LocalTime) {
        viewModel.onAction(SearchAction.CustomTimeEditing(field))
        viewModel.onAction(SearchAction.CustomTimePicked(time))
    }

    @Test
    fun `al verse Buscar ofrece retomar la reserva en curso`() = runTest {
        reservations.activeResult = Result.success(reservation(ReservationStatus.AWAITING_PHONE, id = "activa"))
        val viewModel = newViewModel()

        viewModel.onAction(SearchAction.ScreenStarted)
        assertEquals("activa", viewModel.state.value.activeReservation?.id)

        viewModel.onAction(SearchAction.ActiveReservationClicked)
        assertEquals(SearchEffect.NavigateToReservation("activa"), viewModel.effects.first())
    }

    @Test
    fun `sin reserva en curso, o si terminó, no hay tarjeta`() {
        reservations.activeResult = Result.success(reservation(ReservationStatus.AWAITING_PHONE))
        val viewModel = newViewModel()
        viewModel.onAction(SearchAction.ScreenStarted)

        reservations.activeResult = Result.success(null)
        viewModel.onAction(SearchAction.ScreenStarted)

        assertEquals(null, viewModel.state.value.activeReservation)
    }
}
