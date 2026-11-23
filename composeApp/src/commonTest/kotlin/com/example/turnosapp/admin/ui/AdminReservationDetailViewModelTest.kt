package com.example.turnosapp.admin.ui

import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AdminReservationDetailViewModelTest {

    private val repository = FakeAdminReservationRepository()

    // El turno de prueba es el 26 de noviembre a las 19:30 de Argentina (22:30 UTC).
    private val beforeTheTurn = fixedClock("2026-11-23T15:00:00Z")
    private val afterItStarted = fixedClock("2026-11-26T23:00:00Z")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun fixedClock(instant: String) = object : Clock {
        override fun now(): Instant = Instant.parse(instant)
    }

    private fun viewModelShowing(status: ReservationStatus, clock: Clock = beforeTheTurn): AdminReservationDetailViewModel {
        repository.getResult = Result.success(adminReservation(status))
        return AdminReservationDetailViewModel(repository, "5b1f", clock)
    }

    @Test
    fun `al abrir lee la reserva y ofrece cancelar una confirmada que no empezó`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)

        assertEquals(1, repository.getCalls)
        assertEquals(ReservationStatus.CONFIRMED, viewModel.state.value.reservation?.reservation?.status)
        assertTrue(viewModel.state.value.canCancel)
    }

    @Test
    fun `no ofrece cancelar si el turno ya empezó ni si no está confirmada`() = runTest {
        assertFalse(viewModelShowing(ReservationStatus.CONFIRMED, afterItStarted).state.value.canCancel)
        assertFalse(viewModelShowing(ReservationStatus.AWAITING_PHONE).state.value.canCancel)
        assertFalse(viewModelShowing(ReservationStatus.CANCELLED).state.value.canCancel)
    }

    @Test
    fun `actualizar vuelve a pedir la reserva`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.AWAITING_PHONE)
        repository.getResult = Result.success(adminReservation(ReservationStatus.CONFIRMED))

        viewModel.onAction(AdminReservationDetailAction.Refresh)

        assertEquals(2, repository.getCalls)
        assertEquals(ReservationStatus.CONFIRMED, viewModel.state.value.reservation?.reservation?.status)
    }

    @Test
    fun `sin motivo no se puede confirmar la cancelación`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)
        viewModel.onAction(AdminReservationDetailAction.CancelClicked)
        assertFalse(viewModel.state.value.canConfirmCancel)

        viewModel.onAction(AdminReservationDetailAction.CancelReasonChanged("   "))
        viewModel.onAction(AdminReservationDetailAction.CancelConfirmed)

        assertFalse(viewModel.state.value.canConfirmCancel)
        assertTrue(repository.cancelReasons.isEmpty())
        assertTrue(viewModel.state.value.isCancelDialogOpen)
    }

    @Test
    fun `cancelar con motivo muestra la reserva cancelada y quién la canceló`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)
        viewModel.onAction(AdminReservationDetailAction.CancelClicked)
        viewModel.onAction(AdminReservationDetailAction.CancelReasonChanged("Pidió la baja por teléfono"))

        viewModel.onAction(AdminReservationDetailAction.CancelConfirmed)

        assertEquals(listOf("Pidió la baja por teléfono"), repository.cancelReasons)
        val state = viewModel.state.value
        assertFalse(state.isCancelDialogOpen)
        assertFalse(state.canCancel)
        assertEquals("El administrador admin", state.reservation?.cancelledByLabel())
    }

    @Test
    fun `el motivo no pasa de 500 caracteres`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)
        viewModel.onAction(AdminReservationDetailAction.CancelClicked)

        viewModel.onAction(AdminReservationDetailAction.CancelReasonChanged("x".repeat(600)))

        assertEquals(ADMIN_CANCEL_REASON_MAX_LENGTH, viewModel.state.value.cancelReason.length)
    }

    @Test
    fun `si turnos dice que ya no se puede cancelar, avisa y recarga`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)
        repository.cancelResult = Result.failure(AppError.Business(409, "RESERVATION_NOT_CANCELLABLE"))
        viewModel.onAction(AdminReservationDetailAction.CancelClicked)
        viewModel.onAction(AdminReservationDetailAction.CancelReasonChanged("Pidió la baja"))

        viewModel.onAction(AdminReservationDetailAction.CancelConfirmed)

        assertEquals("Este turno ya no se puede cancelar.", viewModel.state.value.notice)
        assertEquals(2, repository.getCalls)
    }

    @Test
    fun `si la cátedra no está disponible, avisa sin recargar`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)
        repository.cancelResult = Result.failure(AppError.Unavailable)
        viewModel.onAction(AdminReservationDetailAction.CancelClicked)
        viewModel.onAction(AdminReservationDetailAction.CancelReasonChanged("Pidió la baja"))

        viewModel.onAction(AdminReservationDetailAction.CancelConfirmed)

        assertTrue(viewModel.state.value.notice!!.contains("no está disponible"))
        assertEquals(1, repository.getCalls)
        assertTrue(viewModel.state.value.canCancel)
    }

    @Test
    fun `una reserva inexistente muestra el error`() = runTest {
        repository.getResult = Result.failure(AppError.Business(404, "RESERVATION_NOT_FOUND"))

        val viewModel = AdminReservationDetailViewModel(repository, "5b1f", beforeTheTurn)

        assertNull(viewModel.state.value.reservation)
        assertEquals("No se encontró la reserva.", viewModel.state.value.errorMessage)
    }

    @Test
    fun `volver emite NavigateBack`() = runTest {
        val viewModel = viewModelShowing(ReservationStatus.CONFIRMED)

        viewModel.onAction(AdminReservationDetailAction.BackClicked)

        assertEquals(AdminReservationDetailEffect.NavigateBack, viewModel.effects.first())
    }
}
