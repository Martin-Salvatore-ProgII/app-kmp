package com.example.turnosapp.reservation.ui

import com.example.turnosapp.reservation.domain.model.Reservation

/**
 * @param reservation el proceso tal como lo informó turnos la última vez.
 * @param remainingSeconds lo que falta hasta `expiresAt`, con el reloj del teléfono; null si no hay vencimiento.
 * @param totalSeconds lo que faltaba la primera vez que se vio el vencimiento, para la barra de progreso.
 * @param isConnectionLost si la última consulta no llegó: se avisa y se sigue intentando (ADR-0080).
 * @param errorMessage si el proceso no se pudo leer (por ejemplo, no existe): se deja de consultar.
 * @param isPhoneRejected si la cátedra rechazó el último teléfono: se muestra el motivo y se puede enviar otro.
 * @param phone lo que el usuario escribió en el campo del teléfono.
 * @param phoneError por qué no se envió el teléfono (formato inválido, o el servicio no respondió).
 * @param isSubmittingPhone si se está esperando la respuesta del envío.
 */
data class ReservationUiState(
    val phase: ReservationPhase = ReservationPhase.Loading,
    val reservation: Reservation? = null,
    val remainingSeconds: Long? = null,
    val totalSeconds: Long? = null,
    val isConnectionLost: Boolean = false,
    val errorMessage: String? = null,
    val isPhoneRejected: Boolean = false,
    val phone: String = "",
    val phoneError: String? = null,
    val isSubmittingPhone: Boolean = false,
)
