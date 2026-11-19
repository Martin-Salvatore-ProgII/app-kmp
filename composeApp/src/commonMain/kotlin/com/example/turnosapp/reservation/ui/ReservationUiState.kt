package com.example.turnosapp.reservation.ui

import com.example.turnosapp.reservation.domain.model.Reservation

/**
 * @param reservation el proceso tal como lo informó turnos la última vez.
 * @param remainingSeconds lo que falta hasta `expiresAt`, con el reloj del teléfono; null si no hay vencimiento.
 * @param totalSeconds lo que faltaba la primera vez que se vio el vencimiento, para la barra de progreso.
 * @param isConnectionLost si la última consulta no llegó: se avisa y se sigue intentando (ADR-0080).
 * @param errorMessage si el proceso no se pudo leer (por ejemplo, no existe): se deja de consultar.
 */
data class ReservationUiState(
    val phase: ReservationPhase = ReservationPhase.Loading,
    val reservation: Reservation? = null,
    val remainingSeconds: Long? = null,
    val totalSeconds: Long? = null,
    val isConnectionLost: Boolean = false,
    val errorMessage: String? = null,
)
