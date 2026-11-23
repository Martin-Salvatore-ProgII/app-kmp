package com.example.turnosapp.myreservations.ui

import com.example.turnosapp.reservation.domain.model.Reservation

/** El largo máximo del motivo de cancelación (`docs/arq/contratos.md`, cancelar una reserva). */
const val CANCEL_REASON_MAX_LENGTH = 500

/**
 * Todo lo que dibuja el Detalle de una reserva (HU-08, HU-09). El diálogo de cancelar también vive
 * acá: si está abierto y qué motivo lleva escrito.
 *
 * @param canCancel si se muestra el botón: confirmada y con el turno por delante (ADR-0081).
 * @param notice un aviso arriba, por ejemplo que el turno ya no se puede cancelar.
 */
data class ReservationDetailUiState(
    val reservation: Reservation? = null,
    val isLoading: Boolean = false,
    /** No se pudo leer la reserva: no hay nada que mostrar. */
    val errorMessage: String? = null,
    val canCancel: Boolean = false,
    val isCancelDialogOpen: Boolean = false,
    val cancelReason: String = "",
    val isCancelling: Boolean = false,
    val notice: String? = null,
)
