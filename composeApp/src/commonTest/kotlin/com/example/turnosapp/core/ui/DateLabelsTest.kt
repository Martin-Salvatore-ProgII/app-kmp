package com.example.turnosapp.core.ui

import com.example.turnosapp.search.domain.model.DayPeriod
import com.example.turnosapp.search.domain.model.TimeRange
import com.example.turnosapp.search.ui.timeRangeLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateLabelsTest {

    private val thursday = LocalDate(2026, 11, 26)

    @Test
    fun `arma los textos de una fecha en español`() {
        assertEquals("Jue 26 nov", thursday.shortLabel())
        assertEquals("26 nov", thursday.dayMonthLabel())
        assertEquals("Jueves 26 de noviembre", thursday.longLabel())
    }

    @Test
    fun `una hora va con dos dígitos y sin segundos`() {
        assertEquals("09:05", LocalTime(9, 5, 30).label())
    }

    @Test
    fun `la conversión del calendario no corre el día`() {
        assertEquals(thursday, thursday.toPickerMillis().toPickerDate())
    }

    @Test
    fun `una franja fija se muestra por su nombre y una personalizada por sus horas`() {
        assertEquals("Noche", timeRangeLabel(DayPeriod.EVENING.range))
        assertEquals("10:00 a 11:00", timeRangeLabel(TimeRange(LocalTime(10, 0), LocalTime(11, 0))))
    }
}
