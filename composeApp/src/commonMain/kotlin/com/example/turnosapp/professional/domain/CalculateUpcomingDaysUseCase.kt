package com.example.turnosapp.professional.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Las próximas fechas en que atiende un profesional, que la pantalla Profesional ofrece como
 * accesos directos a sus turnos (ADR-0079).
 *
 * Si se buscó con una fecha y el profesional atiende ese día, va primera. Después siguen las
 * próximas fechas desde hoy (incluido, porque puede quedar algún turno a la tarde), sin repetir.
 * Es disponibilidad de agenda: no garantiza turnos libres (ADR-0013).
 */
class CalculateUpcomingDaysUseCase {

    operator fun invoke(
        attendedDays: Set<DayOfWeek>,
        today: LocalDate,
        searchedDate: LocalDate? = null,
        count: Int = DEFAULT_COUNT,
    ): List<LocalDate> {
        if (attendedDays.isEmpty()) return emptyList()
        val days = mutableListOf<LocalDate>()
        if (searchedDate != null && searchedDate >= today && searchedDate.dayOfWeek in attendedDays) {
            days += searchedDate
        }
        var date = today
        while (days.size < count) {
            if (date.dayOfWeek in attendedDays && date != searchedDate) days += date
            date = date.plus(DatePeriod(days = 1))
        }
        return days
    }

    private companion object {
        /** Cuatro tarjetas: dos filas en el celular, sin tener que deslizar. */
        const val DEFAULT_COUNT = 4
    }
}
