package com.example.turnosapp.myreservations.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.turnosapp.reservation.domain.model.ReservationStatus

/**
 * Cómo se nombra cada estado en Mis reservas y en el Detalle. Varios estados internos del proceso se
 * muestran igual: al usuario le importa si se está reservando, si falta su teléfono o cómo terminó,
 * no en qué paso técnico está (`docs/arq/maquina-estados.md`).
 */
fun ReservationStatus.label(): String =
    when (this) {
        ReservationStatus.STARTED, ReservationStatus.HELD, ReservationStatus.AWAITING_REQUEST -> "Reservando…"
        ReservationStatus.AWAITING_PHONE -> "Esperando tu teléfono"
        ReservationStatus.PHONE_SUBMITTED -> "Confirmando…"
        ReservationStatus.CONFIRMED -> "Confirmada"
        ReservationStatus.CANCELLED -> "Cancelada"
        ReservationStatus.EXPIRED -> "Venció sin completar"
        ReservationStatus.FAILED, ReservationStatus.INVALID -> "No se completó"
    }

/** La etiqueta del estado, con el color del tema según cómo está la reserva. */
@Composable
fun ReservationStatusBadge(status: ReservationStatus, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when {
        status == ReservationStatus.CONFIRMED -> colors.secondaryContainer to colors.onSecondaryContainer
        status == ReservationStatus.CANCELLED -> colors.surfaceVariant to colors.onSurfaceVariant
        status.isFinal -> colors.errorContainer to colors.onErrorContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(6.dp), modifier = modifier) {
        Text(status.label(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
