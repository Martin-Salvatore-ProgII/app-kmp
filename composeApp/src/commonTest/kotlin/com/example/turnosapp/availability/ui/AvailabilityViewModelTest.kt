package com.example.turnosapp.availability.ui

import com.example.turnosapp.availability.domain.FindAttendedDayUseCase
import com.example.turnosapp.availability.domain.model.Availability
import com.example.turnosapp.availability.domain.model.Slot
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.professional.domain.model.ProfessionalAgenda
import com.example.turnosapp.professional.domain.model.WeeklySchedule
import com.example.turnosapp.professional.ui.FakeProfessionalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AvailabilityViewModelTest {

    private val today = LocalDate(2026, 11, 19) // jueves
    private val thursday = LocalDate(2026, 11, 26)
    private val rivas = ProfessionalAgenda(
        id = 202,
        firstName = "Martin",
        lastName = "Rivas",
        categoryName = "Cardiologia",
        enabled = true,
        schedules = listOf(
            WeeklySchedule(DayOfWeek.TUESDAY, LocalTime(11, 30), LocalTime(14, 30)),
            WeeklySchedule(DayOfWeek.THURSDAY, LocalTime(18, 0), LocalTime(21, 0)),
        ),
    )
    private val slots = listOf(Slot(LocalTime(18, 0), LocalTime(18, 30)), Slot(LocalTime(19, 30), LocalTime(20, 0)))
    private val availability = FakeAvailabilityRepository { date -> Result.success(availabilityOn(date, slots)) }
    private val professionals = FakeProfessionalRepository(Result.success(rivas))

    private fun availabilityOn(date: LocalDate, slots: List<Slot>) = Availability(202, "Martin", "Rivas", date, slots)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel(date: LocalDate = thursday) =
        AvailabilityViewModel(availability, professionals, FindAttendedDayUseCase(), 202, date, today)

    @Test
    fun `carga los turnos y las flechas van a los días en que atiende`() {
        val state = newViewModel().state.value

        assertEquals(slots, state.slots)
        assertEquals("Martin Rivas", state.professionalName)
        assertEquals("Cardiologia", state.categoryName)
        assertEquals(LocalDate(2026, 11, 24), state.previousDay)
        assertEquals(LocalDate(2026, 12, 1), state.nextDay)
    }

    @Test
    fun `pasar al día siguiente pide los turnos de ese día`() {
        val viewModel = newViewModel()

        viewModel.onAction(AvailabilityAction.NextDayClicked)

        assertEquals(LocalDate(2026, 12, 1), viewModel.state.value.date)
        assertEquals(listOf(thursday, LocalDate(2026, 12, 1)), availability.requestedDates)
    }

    @Test
    fun `en hoy no hay día anterior`() {
        val state = newViewModel(date = today).state.value

        assertNull(state.previousDay)
    }

    @Test
    fun `una fecha fuera del horizonte se muestra como tal y no como error`() {
        availability.result = { Result.failure(AppError.Business(400, "DATE_OUT_OF_RANGE")) }

        val state = newViewModel().state.value

        assertTrue(state.isDateOutOfRange)
        assertNull(state.errorMessage)
    }

    @Test
    fun `sin servicio muestra el mensaje y reintentar vuelve a pedir`() {
        availability.result = { Result.failure(AppError.Unavailable) }
        val viewModel = newViewModel()
        assertEquals(
            "El servicio no está disponible en este momento. Probá de nuevo en unos minutos.",
            viewModel.state.value.errorMessage,
        )

        availability.result = { Result.success(availabilityOn(it, slots)) }
        viewModel.onAction(AvailabilityAction.Retry)

        assertNull(viewModel.state.value.errorMessage)
        assertEquals(slots, viewModel.state.value.slots)
    }

    @Test
    fun `sin la agenda las flechas avanzan día por día`() {
        professionals.result = Result.failure(AppError.Network)

        val state = newViewModel().state.value

        assertEquals(LocalDate(2026, 11, 27), state.nextDay)
        assertEquals(LocalDate(2026, 11, 25), state.previousDay)
    }
}
