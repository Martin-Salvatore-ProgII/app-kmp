package com.example.turnosapp.professional.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculateUpcomingDaysUseCaseTest {

    private val calculate = CalculateUpcomingDaysUseCase()

    // Miércoles 18 de noviembre de 2026. Martín Rivas atiende martes y jueves.
    private val wednesday = LocalDate(2026, 11, 18)
    private val tuesdayAndThursday = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)

    @Test
    fun `sin fecha buscada, ofrece los próximos días que atiende desde hoy`() {
        val days = calculate(tuesdayAndThursday, today = wednesday)

        assertEquals(
            listOf(LocalDate(2026, 11, 19), LocalDate(2026, 11, 24), LocalDate(2026, 11, 26), LocalDate(2026, 12, 1)),
            days,
        )
    }

    @Test
    fun `la fecha buscada va primera y no se repite`() {
        val searched = LocalDate(2026, 11, 26)

        val days = calculate(tuesdayAndThursday, today = wednesday, searchedDate = searched)

        assertEquals(
            listOf(searched, LocalDate(2026, 11, 19), LocalDate(2026, 11, 24), LocalDate(2026, 12, 1)),
            days,
        )
    }

    @Test
    fun `hoy cuenta si atiende hoy`() {
        val days = calculate(setOf(DayOfWeek.WEDNESDAY), today = wednesday, count = 2)

        assertEquals(listOf(wednesday, LocalDate(2026, 11, 25)), days)
    }

    @Test
    fun `una fecha buscada pasada o de un día que no atiende no se ofrece`() {
        val past = LocalDate(2026, 11, 17)
        val friday = LocalDate(2026, 11, 20)

        assertEquals(LocalDate(2026, 11, 19), calculate(tuesdayAndThursday, wednesday, searchedDate = past).first())
        assertEquals(LocalDate(2026, 11, 19), calculate(tuesdayAndThursday, wednesday, searchedDate = friday).first())
    }

    @Test
    fun `sin horarios no hay días`() {
        assertEquals(emptyList(), calculate(emptySet(), today = wednesday))
    }
}
