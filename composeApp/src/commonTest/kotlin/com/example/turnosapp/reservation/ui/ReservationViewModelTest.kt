package com.example.turnosapp.reservation.ui

import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.reservation.domain.ValidatePhoneUseCase
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Un reloj que avanza solo cuando la prueba lo mueve. */
private class FakeClock(var now: Instant) : Clock {
    override fun now(): Instant = now
    fun advance(duration: Duration) {
        now += duration
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReservationViewModelTest {

    private val start = Instant.parse("2026-11-19T14:40:00Z")
    private val clock = FakeClock(start)
    private val repository = FakeReservationRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun followingViewModel() =
        ReservationViewModel(repository, ValidatePhoneUseCase(), "5b1f", clock).also { it.onAction(ReservationAction.ScreenStarted) }

    @Test
    fun `consulta cada 2 segundos hasta un estado final y ahí deja de consultar`() = runTest {
        repository.getResult = Result.success(reservation(ReservationStatus.AWAITING_REQUEST))
        val viewModel = followingViewModel()
        assertEquals(ReservationPhase.Reserving, viewModel.state.value.phase)

        repository.getResult = Result.success(reservation(ReservationStatus.CONFIRMED))
        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)
        assertEquals(ReservationPhase.Confirmed, viewModel.state.value.phase)

        advanceTimeBy(10 * POLL_INTERVAL_MILLIS)
        assertEquals(2, repository.getCalls)
    }

    @Test
    fun `cuenta el tiempo que queda y al llegar a cero ya no pide el teléfono`() = runTest {
        repository.getResult = Result.success(reservation(ReservationStatus.AWAITING_PHONE, expiresAt = start + 90.seconds))
        val viewModel = followingViewModel()
        assertEquals(ReservationPhase.AwaitingPhone, viewModel.state.value.phase)
        assertEquals(90, viewModel.state.value.remainingSeconds)

        clock.advance(91.seconds)
        advanceTimeBy(1_001)

        // La cátedra todavía no informó el vencimiento, pero la app ya no deja enviar el teléfono.
        assertEquals(ReservationPhase.TimeUp, viewModel.state.value.phase)
        assertEquals(0, viewModel.state.value.remainingSeconds)

        repository.getResult = Result.success(reservation(ReservationStatus.EXPIRED, expiresAt = start + 90.seconds))
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        assertEquals(ReservationPhase.Expired, viewModel.state.value.phase)
    }

    @Test
    fun `sin conexión avisa y sigue consultando`() = runTest {
        repository.getResult = Result.failure(AppError.Network)
        val viewModel = followingViewModel()
        assertTrue(viewModel.state.value.isConnectionLost)

        repository.getResult = Result.success(reservation(ReservationStatus.CONFIRMED))
        advanceTimeBy(POLL_INTERVAL_MILLIS + 1)

        assertEquals(false, viewModel.state.value.isConnectionLost)
        assertEquals(ReservationPhase.Confirmed, viewModel.state.value.phase)
    }

    @Test
    fun `con la pantalla oculta no consulta, y al volver consulta en el momento`() = runTest {
        repository.getResult = Result.success(reservation(ReservationStatus.AWAITING_REQUEST))
        val viewModel = followingViewModel()
        viewModel.onAction(ReservationAction.ScreenStopped)

        advanceTimeBy(10 * POLL_INTERVAL_MILLIS)
        assertEquals(1, repository.getCalls)

        repository.getResult = Result.success(reservation(ReservationStatus.CONFIRMED))
        viewModel.onAction(ReservationAction.ScreenStarted)
        assertEquals(2, repository.getCalls)
        assertEquals(ReservationPhase.Confirmed, viewModel.state.value.phase)
    }

    @Test
    fun `un rechazo muestra el mensaje propio de su motivo`() {
        val failed = reservation(ReservationStatus.FAILED, failureReason = "INTEGRATION_UNAVAILABLE")

        assertEquals(
            ReservationPhase.Failed("No pudimos reservar en este momento. Probá de nuevo en unos minutos."),
            phaseOf(failed, remainingSeconds = null),
        )
        assertEquals(
            ReservationPhase.Failed("No se pudo completar la reserva. Probá de nuevo."),
            phaseOf(reservation(ReservationStatus.INVALID, failureReason = "REQUEST_EVENT_MISMATCH"), remainingSeconds = null),
        )
    }

    @Test
    fun `un proceso que no existe deja de consultarse`() = runTest {
        repository.getResult = Result.failure(AppError.Business(404, "RESERVATION_NOT_FOUND"))
        val viewModel = followingViewModel()

        advanceTimeBy(10 * POLL_INTERVAL_MILLIS)

        assertEquals("No se encontró la reserva.", viewModel.state.value.errorMessage)
        assertEquals(1, repository.getCalls)
    }

    private fun awaitingPhone(failureReason: String? = null) =
        reservation(ReservationStatus.AWAITING_PHONE, expiresAt = start + 300.seconds, failureReason = failureReason)

    @Test
    fun `un teléfono válido se envía y pasa a confirmando`() = runTest {
        repository.getResult = Result.success(awaitingPhone())
        val viewModel = followingViewModel()

        viewModel.onAction(ReservationAction.PhoneChanged("+54 9 11 5555-1234"))
        viewModel.onAction(ReservationAction.SubmitPhoneClicked)

        assertEquals(listOf("+54 9 11 5555-1234"), repository.submittedPhones)
        assertEquals(ReservationPhase.Confirming, viewModel.state.value.phase)
        viewModel.onAction(ReservationAction.ScreenStopped)
    }

    @Test
    fun `un teléfono con formato inválido no se envía`() = runTest {
        repository.getResult = Result.success(awaitingPhone())
        val viewModel = followingViewModel()

        viewModel.onAction(ReservationAction.PhoneChanged("11 5555"))
        viewModel.onAction(ReservationAction.SubmitPhoneClicked)

        assertEquals(emptyList(), repository.submittedPhones)
        assertEquals("Tiene que tener entre 7 y 15 números. Puede empezar con +.", viewModel.state.value.phoneError)
        viewModel.onAction(ReservationAction.ScreenStopped)
    }

    @Test
    fun `sin tiempo no se envía el teléfono aunque el estado lo pida`() = runTest {
        repository.getResult = Result.success(awaitingPhone())
        val viewModel = followingViewModel()
        viewModel.onAction(ReservationAction.PhoneChanged("+54 9 11 5555-1234"))

        clock.advance(301.seconds)
        advanceTimeBy(1_001)
        viewModel.onAction(ReservationAction.SubmitPhoneClicked)

        assertEquals(ReservationPhase.TimeUp, viewModel.state.value.phase)
        assertEquals(emptyList(), repository.submittedPhones)
        viewModel.onAction(ReservationAction.ScreenStopped)
    }

    @Test
    fun `un rechazo de la cátedra se muestra y deja enviar otro`() = runTest {
        repository.getResult = Result.success(awaitingPhone(failureReason = "INVALID_PHONE_NUMBER"))
        val viewModel = followingViewModel()

        assertTrue(viewModel.state.value.isPhoneRejected)
        assertEquals(ReservationPhase.AwaitingPhone, viewModel.state.value.phase)
        viewModel.onAction(ReservationAction.ScreenStopped)
    }
}
