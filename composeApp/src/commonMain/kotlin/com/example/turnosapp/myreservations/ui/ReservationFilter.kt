package com.example.turnosapp.myreservations.ui

import com.example.turnosapp.reservation.domain.model.ReservationStatus

/**
 * Los chips de Mis reservas (ADR-0081). Cada uno es un valor del filtro `status` de turnos, que
 * acepta uno solo, o ninguno para todas.
 */
enum class ReservationFilter(val label: String, val status: ReservationStatus?) {
    ALL("Todas", null),
    CONFIRMED("Confirmadas", ReservationStatus.CONFIRMED),
    CANCELLED("Canceladas", ReservationStatus.CANCELLED),
}
