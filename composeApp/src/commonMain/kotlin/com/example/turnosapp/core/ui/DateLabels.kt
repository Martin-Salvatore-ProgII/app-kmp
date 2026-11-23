package com.example.turnosapp.core.ui

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number

// Textos de fechas y horas en español. Escritos a mano porque kotlinx-datetime no traduce nombres
// de días ni de meses, y son pocos.

private val dayNames = mapOf(
    DayOfWeek.MONDAY to "Lunes",
    DayOfWeek.TUESDAY to "Martes",
    DayOfWeek.WEDNESDAY to "Miércoles",
    DayOfWeek.THURSDAY to "Jueves",
    DayOfWeek.FRIDAY to "Viernes",
    DayOfWeek.SATURDAY to "Sábado",
    DayOfWeek.SUNDAY to "Domingo",
)

private val monthNames = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** "Jueves". */
fun DayOfWeek.label(): String = dayNames.getValue(this)

/** "Jue 26 nov", para un chip. */
fun LocalDate.shortLabel(): String =
    "${dayOfWeek.label().take(3)} $day ${monthNames[month.number - 1].take(3)}"

/** "26 nov", para una tarjeta que ya muestra el día de la semana. */
fun LocalDate.dayMonthLabel(): String = "$day ${monthNames[month.number - 1].take(3)}"

/** "Jueves 26 de noviembre". */
fun LocalDate.longLabel(): String = "${dayOfWeek.label()} $day de ${monthNames[month.number - 1]}"

/** "18:00". Sin segundos: la agenda no los usa. */
fun LocalTime.label(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

/** "nov", para el bloque de fecha de una tarjeta. */
fun LocalDate.monthShortLabel(): String = monthNames[month.number - 1].take(3)
