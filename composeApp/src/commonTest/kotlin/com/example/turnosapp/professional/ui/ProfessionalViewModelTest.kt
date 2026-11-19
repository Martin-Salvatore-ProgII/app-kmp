package com.example.turnosapp.professional.ui

import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.professional.data.ProfessionalRepository
import com.example.turnosapp.professional.domain.CalculateUpcomingDaysUseCase
import com.example.turnosapp.professional.domain.model.ProfessionalAgenda
import com.example.turnosapp.professional.domain.model.WeeklySchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProfessionalViewModelTest {

    private val today = LocalDate(2026, 11, 18)
    private val thursday = LocalDate(2026, 11, 26)
    private val rivas = ProfessionalAgenda(
        id = 202,
        firstName = "Martin",
        lastName = "Rivas",
        categoryName = "Cardiologia",
        enabled = true,
        schedules = listOf(WeeklySchedule(DayOfWeek.THURSDAY, LocalTime(18, 0), LocalTime(21, 0))),
    )
    private val repository = FakeProfessionalRepository(Result.success(rivas))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel(searchedDate: LocalDate? = null) =
        ProfessionalViewModel(repository, CalculateUpcomingDaysUseCase(), professionalId = 202, searchedDate, today)

    @Test
    fun `carga la agenda y ofrece primero el día buscado`() {
        val state = newViewModel(searchedDate = thursday).state.value

        assertEquals(rivas, state.agenda)
        assertEquals(thursday, state.upcomingDays.first())
        assertEquals(4, state.upcomingDays.size)
    }

    @Test
    fun `elegir un día lleva a sus turnos`() = runTest {
        val viewModel = newViewModel()

        viewModel.onAction(ProfessionalAction.DayChosen(thursday))

        assertEquals(ProfessionalEffect.NavigateToAvailability(202, thursday), viewModel.effects.first())
    }

    @Test
    fun `un profesional no habilitado no tiene accesos a turnos`() {
        repository.result = Result.success(rivas.copy(enabled = false))

        val state = newViewModel(searchedDate = thursday).state.value

        assertEquals(emptyList(), state.upcomingDays)
    }

    @Test
    fun `un profesional inexistente muestra su mensaje y se puede reintentar`() {
        repository.result = Result.failure(AppError.Business(404, "PROFESSIONAL_NOT_FOUND"))
        val viewModel = newViewModel()
        assertEquals("No se encontró el profesional.", viewModel.state.value.errorMessage)

        repository.result = Result.success(rivas)
        viewModel.onAction(ProfessionalAction.Retry)

        assertNull(viewModel.state.value.errorMessage)
        assertEquals(rivas, viewModel.state.value.agenda)
    }

    @Test
    fun `elegir otra fecha abre el calendario y cancelarlo lo cierra`() {
        val viewModel = newViewModel()

        viewModel.onAction(ProfessionalAction.OtherDateClicked)
        assertEquals(true, viewModel.state.value.isPickingDate)

        viewModel.onAction(ProfessionalAction.DatePickerDismissed)
        assertEquals(false, viewModel.state.value.isPickingDate)
    }
}

private class FakeProfessionalRepository(var result: Result<ProfessionalAgenda>) : ProfessionalRepository {
    override suspend fun getAgenda(professionalId: Long): Result<ProfessionalAgenda> = result
}
