package com.example.turnosapp.availability.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * El día anterior o siguiente en que atiende un profesional, para las flechas de Turnos
 * disponibles (ADR-0080). Se busca a lo sumo una semana: si atiende algún día, aparece.
 *
 * Sin días de atención conocidos (la agenda no se pudo leer), las flechas avanzan día por día.
 */
class FindAttendedDayUseCase {

    fun next(attendedDays: Set<DayOfWeek>, from: LocalDate): LocalDate {
        for (i in 1..DAYS_IN_WEEK) {
            val date = from.plus(DatePeriod(days = i))
            if (date.dayOfWeek in attendedDays) return date
        }
        return from.plus(DatePeriod(days = 1))
    }

    /** Null si no hay un día anterior que no haya pasado: la flecha queda deshabilitada. */
    fun previous(attendedDays: Set<DayOfWeek>, from: LocalDate, today: LocalDate): LocalDate? {
        for (i in 1..DAYS_IN_WEEK) {
            val date = from.minus(DatePeriod(days = i))
            if (date < today) return null
            if (date.dayOfWeek in attendedDays || attendedDays.isEmpty()) return date
        }
        return null
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
