package com.example.turnosapp.search.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Los cuatro filtros de la búsqueda (HU-05): categoría, nombre, estado habilitado y disponibilidad
 * de agenda, que es una fecha con una franja horaria opcional (ADR-0013).
 *
 * @param enabled `true` busca los habilitados y `false` solo los no habilitados: el contrato no
 * tiene la opción "todos" (ADR-0014).
 * @param timeRange la franja; solo tiene sentido con [date] (ADR-0065).
 */
data class SearchFilters(
    val name: String = "",
    val category: Category? = null,
    val date: LocalDate? = null,
    val timeRange: TimeRange? = null,
    val enabled: Boolean = true,
)

/** Una franja horaria completa, con su inicio antes del fin (ADR-0065). */
data class TimeRange(
    val from: LocalTime,
    val to: LocalTime,
)

/**
 * Las franjas fijas de la búsqueda (ADR-0079). Son una preferencia de la interfaz, no un valor del
 * backend. La mañana empieza a las 0 para no esconder un horario temprano, y la noche termina a las
 * 23:59 porque 24:00 no es una hora válida.
 */
enum class DayPeriod(val range: TimeRange) {
    MORNING(TimeRange(LocalTime(0, 0), LocalTime(12, 0))),
    AFTERNOON(TimeRange(LocalTime(12, 0), LocalTime(18, 0))),
    EVENING(TimeRange(LocalTime(18, 0), LocalTime(23, 59))),
}
