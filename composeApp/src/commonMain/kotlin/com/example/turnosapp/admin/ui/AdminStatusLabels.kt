package com.example.turnosapp.admin.ui

import com.example.turnosapp.reservation.domain.model.ReservationStatus

/**
 * El nombre de cada estado para el administrador. A diferencia de Mis reservas, no agrupa los pasos
 * en curso: el administrador busca justamente el paso en que quedó un proceso (ADR-0082).
 */
fun ReservationStatus.adminLabel(): String =
    when (this) {
        ReservationStatus.STARTED -> "Iniciada"
        ReservationStatus.HELD -> "Turno retenido"
        ReservationStatus.AWAITING_REQUEST -> "Esperando pedido de teléfono"
        ReservationStatus.AWAITING_PHONE -> "Esperando teléfono"
        ReservationStatus.PHONE_SUBMITTED -> "Teléfono enviado"
        ReservationStatus.CONFIRMED -> "Confirmada"
        ReservationStatus.CANCELLED -> "Cancelada"
        ReservationStatus.EXPIRED -> "Vencida"
        ReservationStatus.INVALID -> "Inválida"
        ReservationStatus.FAILED -> "Fallida"
    }
