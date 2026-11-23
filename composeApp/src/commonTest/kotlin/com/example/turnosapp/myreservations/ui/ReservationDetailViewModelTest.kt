package com.example.turnosapp.myreservations.ui

import com.example.turnosapp.core.error.AppError
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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** El turno de prueba es el jueves 26 de noviembre a las 19:30 de Argentina. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReservationDetailViewModelTest {

    private val repository = FakeReservationRepository()
    private val beforeTheTurn = fixedClock("2026-11-23T15:00:00Z")
    private val afterItStarted = fixedClock("2026-11-26T23:00:00Z")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun fixedClock(instant: String) = object : Clock {
        override fun now() = Instant.parse(instant)
    }

    private fun viewModelShowing(status: ReservationStatus, clock: Clock = beforeTheTurn): ReservationDetailViewModel {
        repository.getResult = Result.success(reservation(status))
        return ReservationDetailViewModel(repository, "5b1f", clock)
    }

    @Test
    fun `una reserva confirmada con el turno por delante ofrece cancelar`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)

        assertEquals(ReservationStatus.CONFIRMED, viewModel.state.value.reservation?.status)
        assertTrue(viewModel.state.value.canCancel)
    }

    @Test
    fun `si el turno ya empezó no ofrece cancelar`() = runTest {
        assertFalse(viewModelShowing(ReservationStatus.CONFIRMED, afterItStarted).state.value.canCancel)
    }

    @Test
    fun `una reserva que no está confirmada no ofrece cancelar ni abre el diálogo`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.EXPIRED)

        viewModel.onAction(ReservationDetailAction.CancelClicked)

        assertFalse(viewModel.state.value.canCancel)
        assertFalse(viewModel.state.value.isCancelDialogOpen)
    }

    @Test
    fun `cancelar envía el motivo y muestra la reserva cancelada`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)
        repository.cancelResult = Result.success(reservation(ReservationStatus.CANCELLED, cancellationReason = "No llego"))

        viewModel.onAction(ReservationDetailAction.CancelClicked)
        viewModel.onAction(ReservationDetailAction.CancelReasonChanged("No llego"))
        viewModel.onAction(ReservationDetailAction.CancelConfirmed)

        assertEquals(listOf("No llego"), repository.cancelReasons)
        val state = viewModel.state.value
        assertEquals(ReservationStatus.CANCELLED, state.reservation?.status)
        assertFalse(state.isCancelDialogOpen)
        assertFalse(state.canCancel)
        assertNull(state.notice)
    }

    @Test
    fun `el motivo no pasa de 500 caracteres`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)

        viewModel.onAction(ReservationDetailAction.CancelReasonChanged("a".repeat(600)))

        assertEquals(CANCEL_REASON_MAX_LENGTH, viewModel.state.value.cancelReason.length)
    }

    @Test
    fun `si turnos dice que ya no se puede cancelar, avisa y recarga la reserva`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)
        repository.cancelResult = Result.failure(AppError.Business(409, "RESERVATION_NOT_CANCELLABLE"))
        val getsBefore = repository.getCalls

        viewModel.onAction(ReservationDetailAction.CancelClicked)
        viewModel.onAction(ReservationDetailAction.CancelConfirmed)

        assertEquals("Este turno ya no se puede cancelar.", viewModel.state.value.notice)
        assertEquals(getsBefore + 1, repository.getCalls)
        assertFalse(viewModel.state.value.isCancelDialogOpen)
    }

    @Test
    fun `si la cátedra no está disponible, avisa y la reserva sigue confirmada`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)
        repository.cancelResult = Result.failure(AppError.Unavailable)

        viewModel.onAction(ReservationDetailAction.CancelClicked)
        viewModel.onAction(ReservationDetailAction.CancelConfirmed)

        assertNotNull(viewModel.state.value.notice)
        assertEquals(ReservationStatus.CONFIRMED, viewModel.state.value.reservation?.status)
        assertTrue(viewModel.state.value.canCancel)
    }

    @Test
    fun `si no se puede leer la reserva, muestra el error y Reintentar vuelve a pedirla`() = runTest {
        repository.getResult = Result.failure(AppError.Business(404, "RESERVATION_NOT_FOUND"))
        val viewModel = ReservationDetailViewModel(repository, "5b1f", beforeTheTurn)
        assertEquals("No se encontró la reserva.", viewModel.state.value.errorMessage)

        repository.getResult = Result.success(reservation(ReservationStatus.CONFIRMED))
        viewModel.onAction(ReservationDetailAction.Retry)

        assertNull(viewModel.state.value.errorMessage)
        assertNotNull(viewModel.state.value.reservation)
    }

    @Test
    fun `Buscar otro turno lleva a Buscar`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CANCELLED)

        viewModel.onAction(ReservationDetailAction.SearchClicked)

        assertEquals(ReservationDetailEffect.NavigateToSearch, viewModel.effects.first())
    }
}
