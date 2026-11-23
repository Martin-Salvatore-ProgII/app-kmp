package com.example.turnosapp.admin.domain.model

import com.example.turnosapp.reservation.domain.model.ReservationStatus
import kotlinx.datetime.LocalDate

/**
 * Los filtros de la lista de administración (ADR-0082). Todos son opcionales y se suman; null es "sin
 * ese filtro".
 *
 * @param ownerLogin el login completo del dueño: turnos no busca por parte del texto.
 * @param from el primer día del turno, incluido.
 * @param to el último día del turno, incluido.
 */
data class AdminReservationFilters(
    val ownerLogin: String? = null,
    val status: ReservationStatus? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
)
