package com.example.turnosapp.availability.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FindAttendedDayUseCaseTest {

    private val findAttendedDay = FindAttendedDayUseCase()
    private val tuesdaysAndThursdays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
    private val today = LocalDate(2026, 11, 19) // jueves

    @Test
    fun `el siguiente salta a los días en que atiende`() {
        assertEquals(LocalDate(2026, 11, 24), findAttendedDay.next(tuesdaysAndThursdays, today))
        assertEquals(LocalDate(2026, 11, 26), findAttendedDay.next(tuesdaysAndThursdays, LocalDate(2026, 11, 24)))
    }

    @Test
    fun `el anterior salta a los días en que atiende sin pasar de hoy`() {
        assertEquals(today, findAttendedDay.previous(tuesdaysAndThursdays, LocalDate(2026, 11, 24), today))
        assertNull(findAttendedDay.previous(tuesdaysAndThursdays, today, today))
        // El martes 17 ya pasó: desde el lunes 23 no hay un día anterior que ofrecer.
        assertNull(findAttendedDay.previous(tuesdaysAndThursdays, LocalDate(2026, 11, 23), LocalDate(2026, 11, 20)))
    }

    @Test
    fun `sin días de atención conocidos avanza día por día`() {
        assertEquals(LocalDate(2026, 11, 20), findAttendedDay.next(emptySet(), today))
        assertEquals(LocalDate(2026, 11, 20), findAttendedDay.previous(emptySet(), LocalDate(2026, 11, 21), today))
    }
}
