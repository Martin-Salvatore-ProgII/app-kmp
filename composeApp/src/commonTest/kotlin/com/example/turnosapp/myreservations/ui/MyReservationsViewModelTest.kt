package com.example.turnosapp.myreservations.ui

import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.model.Page
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import com.example.turnosapp.reservation.ui.FakeReservationRepository
import com.example.turnosapp.reservation.ui.reservation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class MyReservationsViewModelTest {

    private val repository = FakeReservationRepository()
    private val confirmed = reservation(ReservationStatus.CONFIRMED, id = "a")
    private val cancelled = reservation(ReservationStatus.CANCELLED, id = "b")
    private val awaitingPhone = reservation(ReservationStatus.AWAITING_PHONE, id = "c")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `no carga hasta que la pantalla se muestra, y entonces pide todas desde la primera página`() = runTest {
        repository.listResults[0] = Result.success(Page(listOf(awaitingPhone, confirmed), 2))
        val viewModel = MyReservationsViewModel(repository)
        assertTrue(repository.listCalls.isEmpty())

        viewModel.onAction(MyReservationsAction.ScreenStarted)

        assertEquals(listOf<Pair<ReservationStatus?, Int>>(null to 0), repository.listCalls)
        assertEquals(listOf(awaitingPhone, confirmed), viewModel.state.value.reservations)
        assertFalse(viewModel.state.value.canLoadMore)
    }

    @Test
    fun `cada vez que se vuelve a mostrar recarga con el filtro elegido`() = runTest {
        val viewModel = MyReservationsViewModel(repository)
        viewModel.onAction(MyReservationsAction.FilterSelected(ReservationFilter.CANCELLED))
        repository.listResults[0] = Result.success(Page(listOf(cancelled), 1))

        viewModel.onAction(MyReservationsAction.ScreenStarted)

        assertEquals(listOf<Pair<ReservationStatus?, Int>>(ReservationStatus.CANCELLED to 0, ReservationStatus.CANCELLED to 0), repository.listCalls)
        assertEquals(listOf(cancelled), viewModel.state.value.reservations)
    }

    @Test
    fun `elegir el filtro que ya está no vuelve a pedir`() = runTest {
        val viewModel = MyReservationsViewModel(repository)

        viewModel.onAction(MyReservationsAction.FilterSelected(ReservationFilter.ALL))

        assertTrue(repository.listCalls.isEmpty())
    }

    @Test
    fun `cargar más suma la página siguiente`() = runTest {
        repository.listResults[0] = Result.success(Page(listOf(confirmed), 2))
        repository.listResults[1] = Result.success(Page(listOf(cancelled), 2))
        val viewModel = MyReservationsViewModel(repository)
        viewModel.onAction(MyReservationsAction.ScreenStarted)
        assertTrue(viewModel.state.value.canLoadMore)

        viewModel.onAction(MyReservationsAction.LoadMore)

        assertEquals(listOf<Pair<ReservationStatus?, Int>>(null to 0, null to 1), repository.listCalls)
        assertEquals(listOf(confirmed, cancelled), viewModel.state.value.reservations)
        assertFalse(viewModel.state.value.canLoadMore)
    }

    @Test
    fun `si falla la página siguiente la lista queda y se puede reintentar`() = runTest {
        repository.listResults[0] = Result.success(Page(listOf(confirmed), 2))
        repository.listResults[1] = Result.failure(AppError.Network)
        val viewModel = MyReservationsViewModel(repository)
        viewModel.onAction(MyReservationsAction.ScreenStarted)

        viewModel.onAction(MyReservationsAction.LoadMore)

        assertEquals(listOf(confirmed), viewModel.state.value.reservations)
        assertNotNull(viewModel.state.value.loadMoreErrorMessage)
    }

    @Test
    fun `si falla la primera página muestra el error y Reintentar vuelve a pedir`() = runTest {
        repository.listResults[0] = Result.failure(AppError.Unavailable)
        val viewModel = MyReservationsViewModel(repository)
        viewModel.onAction(MyReservationsAction.ScreenStarted)
        assertNotNull(viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isEmpty)

        repository.listResults[0] = Result.success(Page(listOf(confirmed), 1))
        viewModel.onAction(MyReservationsAction.Retry)

        assertNull(viewModel.state.value.errorMessage)
        assertEquals(listOf(confirmed), viewModel.state.value.reservations)
    }

    @Test
    fun `sin reservas queda vacía recién después de cargar`() = runTest {
        val viewModel = MyReservationsViewModel(repository)
        assertFalse(viewModel.state.value.isEmpty)

        viewModel.onAction(MyReservationsAction.ScreenStarted)

        assertTrue(viewModel.state.value.isEmpty)
    }

    @Test
    fun `tocar una reserva en curso lleva a Reserva en curso`() = runTest {
        val viewModel = MyReservationsViewModel(repository)

        viewModel.onAction(MyReservationsAction.ReservationClicked(awaitingPhone))

        assertEquals(MyReservationsEffect.NavigateToReservation("c"), viewModel.effects.first())
    }

    @Test
    fun `tocar una reserva terminada lleva al Detalle`() = runTest {
        val viewModel = MyReservationsViewModel(repository)

        viewModel.onAction(MyReservationsAction.ReservationClicked(cancelled))

        assertEquals(MyReservationsEffect.NavigateToDetail("b"), viewModel.effects.first())
    }

    @Test
    fun `cada estado tiene su nombre para el usuario`() {
        assertEquals("Reservando…", ReservationStatus.HELD.label())
        assertEquals("Esperando tu teléfono", ReservationStatus.AWAITING_PHONE.label())
        assertEquals("Venció sin completar", ReservationStatus.EXPIRED.label())
        assertEquals("No se completó", ReservationStatus.INVALID.label())
    }
}
