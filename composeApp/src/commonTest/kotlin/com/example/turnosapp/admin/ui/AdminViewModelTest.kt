package com.example.turnosapp.admin.ui

import com.example.turnosapp.admin.domain.model.AdminReservationFilters
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.model.Page
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val repository = FakeAdminReservationRepository()
    private val confirmed = adminReservation(ReservationStatus.CONFIRMED, id = "a")
    private val failed = adminReservation(ReservationStatus.FAILED, id = "b")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `al mostrarse pide la primera página sin filtros`() = runTest {
        repository.listResults[0] = Result.success(Page(listOf(confirmed, failed), 2))
        val viewModel = AdminViewModel(repository)
        assertTrue(repository.listCalls.isEmpty())

        viewModel.onAction(AdminAction.ScreenStarted)

        assertEquals(listOf(AdminReservationFilters() to 0), repository.listCalls)
        assertEquals(listOf(confirmed, failed), viewModel.state.value.reservations)
        assertFalse(viewModel.state.value.canLoadMore)
    }

    @Test
    fun `escribir el usuario no busca, y buscar aplica el login sin espacios`() = runTest {
        val viewModel = AdminViewModel(repository)

        viewModel.onAction(AdminAction.OwnerChanged(" jperez "))
        assertTrue(repository.listCalls.isEmpty())

        viewModel.onAction(AdminAction.OwnerSubmitted)
        assertEquals(listOf(AdminReservationFilters(ownerLogin = "jperez") to 0), repository.listCalls)
    }

    @Test
    fun `buscar con el campo vacío quita el filtro de usuario`() = runTest {
        val viewModel = AdminViewModel(repository)
        viewModel.onAction(AdminAction.OwnerChanged("jperez"))
        viewModel.onAction(AdminAction.OwnerSubmitted)

        viewModel.onAction(AdminAction.OwnerChanged("   "))
        viewModel.onAction(AdminAction.OwnerSubmitted)

        assertNull(viewModel.state.value.filters.ownerLogin)
        assertEquals(AdminReservationFilters() to 0, repository.listCalls.last())
    }

    @Test
    fun `borrar el usuario lo quita del campo y de los filtros`() = runTest {
        val viewModel = AdminViewModel(repository)
        viewModel.onAction(AdminAction.OwnerChanged("jperez"))
        viewModel.onAction(AdminAction.OwnerSubmitted)

        viewModel.onAction(AdminAction.OwnerCleared)

        assertEquals("", viewModel.state.value.ownerInput)
        assertEquals(AdminReservationFilters() to 0, repository.listCalls.last())
    }

    @Test
    fun `los filtros se suman y cada cambio vuelve a la primera página`() = runTest {
        val viewModel = AdminViewModel(repository)
        val from = LocalDate(2026, 11, 1)
        val to = LocalDate(2026, 11, 30)

        viewModel.onAction(AdminAction.PickerOpened(AdminPicker.STATUS))
        viewModel.onAction(AdminAction.StatusSelected(ReservationStatus.FAILED))
        viewModel.onAction(AdminAction.FromSelected(from))
        viewModel.onAction(AdminAction.ToSelected(to))

        assertEquals(
            listOf(
                AdminReservationFilters(status = ReservationStatus.FAILED) to 0,
                AdminReservationFilters(status = ReservationStatus.FAILED, from = from) to 0,
                AdminReservationFilters(status = ReservationStatus.FAILED, from = from, to = to) to 0,
            ),
            repository.listCalls,
        )
        assertNull(viewModel.state.value.openPicker)
        assertTrue(viewModel.state.value.hasFilters)
    }

    @Test
    fun `elegir el mismo valor solo cierra la hoja, sin volver a pedir`() = runTest {
        val viewModel = AdminViewModel(repository)
        viewModel.onAction(AdminAction.PickerOpened(AdminPicker.STATUS))

        viewModel.onAction(AdminAction.StatusSelected(null))

        assertNull(viewModel.state.value.openPicker)
        assertTrue(repository.listCalls.isEmpty())
    }

    @Test
    fun `el calendario no deja un Desde posterior al Hasta ni al revés`() = runTest {
        val viewModel = AdminViewModel(repository)
        viewModel.onAction(AdminAction.FromSelected(LocalDate(2026, 11, 10)))
        viewModel.onAction(AdminAction.ToSelected(LocalDate(2026, 11, 20)))
        val state = viewModel.state.value

        assertTrue(state.canPickFrom(LocalDate(2026, 11, 20)))
        assertFalse(state.canPickFrom(LocalDate(2026, 11, 21)))
        assertTrue(state.canPickTo(LocalDate(2026, 11, 10)))
        assertFalse(state.canPickTo(LocalDate(2026, 11, 9)))
    }

    @Test
    fun `cargar más suma la página siguiente con los mismos filtros`() = runTest {
        repository.listResults[0] = Result.success(Page(listOf(confirmed), 2))
        repository.listResults[1] = Result.success(Page(listOf(failed), 2))
        val viewModel = AdminViewModel(repository)
        viewModel.onAction(AdminAction.StatusSelected(ReservationStatus.CONFIRMED))

        viewModel.onAction(AdminAction.LoadMore)

        assertEquals(AdminReservationFilters(status = ReservationStatus.CONFIRMED) to 1, repository.listCalls.last())
        assertEquals(listOf(confirmed, failed), viewModel.state.value.reservations)
        assertFalse(viewModel.state.value.canLoadMore)
    }

    @Test
    fun `sin resultados con filtros se distingue de que no haya ninguna reserva`() = runTest {
        val viewModel = AdminViewModel(repository)

        viewModel.onAction(AdminAction.ScreenStarted)
        assertTrue(viewModel.state.value.isEmpty)
        assertFalse(viewModel.state.value.hasFilters)

        viewModel.onAction(AdminAction.StatusSelected(ReservationStatus.INVALID))
        assertTrue(viewModel.state.value.isEmpty)
        assertTrue(viewModel.state.value.hasFilters)
    }

    @Test
    fun `un 403 muestra que no tiene permiso`() = runTest {
        repository.listResults[0] = Result.failure(AppError.Forbidden)
        val viewModel = AdminViewModel(repository)

        viewModel.onAction(AdminAction.ScreenStarted)

        assertEquals("No tenés permiso para hacer esto.", viewModel.state.value.errorMessage)
        assertTrue(viewModel.state.value.reservations.isEmpty())
    }

    @Test
    fun `cada estado tiene su nombre exacto para el administrador`() {
        assertEquals("Esperando pedido de teléfono", ReservationStatus.AWAITING_REQUEST.adminLabel())
        assertEquals("Vencida", ReservationStatus.EXPIRED.adminLabel())
        assertEquals("Inválida", ReservationStatus.INVALID.adminLabel())
        assertEquals("Fallida", ReservationStatus.FAILED.adminLabel())
    }

    @Test
    fun `toda reserva abre el detalle admin, también una en curso`() = runTest {
        val viewModel = AdminViewModel(repository)

        viewModel.onAction(AdminAction.ReservationClicked("c"))

        assertEquals(AdminEffect.NavigateToDetail("c"), viewModel.effects.first())
    }
}
