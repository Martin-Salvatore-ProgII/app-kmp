package com.example.turnosapp.reservation.ui

/**
 * Qué muestra Reserva en curso, según el estado del proceso (columna "La app muestra" de
 * `docs/arq/maquina-estados.md`) y el tiempo que queda (ADR-0080).
 *
 * @param step el paso del indicador: 1 Reservado, 2 Teléfono, 3 Confirmado; 4 si están todos hechos.
 */
sealed class ReservationPhase(val step: Int) {
    /** Todavía no se leyó el proceso. */
    data object Loading : ReservationPhase(1)

    /** `STARTED`, `HELD` o `AWAITING_REQUEST`: se espera el pedido de teléfono. */
    data object Reserving : ReservationPhase(1)

    /** `AWAITING_PHONE` con tiempo: se pide el teléfono. */
    data object AwaitingPhone : ReservationPhase(2)

    /** `AWAITING_PHONE` sin tiempo: ya no se puede enviar el teléfono, se espera el resultado de la cátedra. */
    data object TimeUp : ReservationPhase(2)

    /** `PHONE_SUBMITTED`: se espera la confirmación. */
    data object Confirming : ReservationPhase(3)

    data object Confirmed : ReservationPhase(4)

    data object Cancelled : ReservationPhase(4)

    data object Expired : ReservationPhase(2)

    /** `FAILED` o `INVALID`, con el mensaje propio de su motivo. */
    data class Failed(val message: String) : ReservationPhase(1)

    val isFinal: Boolean get() = this is Confirmed || this is Cancelled || this is Expired || this is Failed
}
