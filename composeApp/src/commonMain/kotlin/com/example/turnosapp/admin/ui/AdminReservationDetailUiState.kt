package com.example.turnosapp.admin.ui

import com.example.turnosapp.admin.domain.model.AdminReservation

/** El largo máximo del motivo de cancelación (`docs/arq/contratos.md`, Administración). */
const val ADMIN_CANCEL_REASON_MAX_LENGTH = 500

/**
 * Todo lo que dibuja el detalle admin de una reserva (HU-10). El diálogo de cancelar también vive acá.
 *
 * @param canCancel si se muestra el botón: confirmada y con el turno por delante, igual que para el dueño.
 * @param notice un aviso arriba, por ejemplo que el turno ya no se puede cancelar.
 */
data class AdminReservationDetailUiState(
    val reservation: AdminReservation? = null,
    val isLoading: Boolean = false,
    /** No se pudo leer la reserva: no hay nada que mostrar. */
    val errorMessage: String? = null,
    val canCancel: Boolean = false,
    val isCancelDialogOpen: Boolean = false,
    val cancelReason: String = "",
    val isCancelling: Boolean = false,
    val notice: String? = null,
) {
    /** El motivo es obligatorio (HU-10, criterio 4): sin él, el botón del diálogo no se habilita. */
    val canConfirmCancel: Boolean get() = cancelReason.isNotBlank() && !isCancelling
}
