package com.example.turnosapp.admin.domain.model

import com.example.turnosapp.reservation.domain.model.Reservation

/**
 * Una reserva vista por un administrador (`docs/arq/contratos.md`, Administración): la misma que ve
 * su dueño, más los datos de soporte de HU-10.
 *
 * @param reservationProcessId el proceso en la cátedra, o null si el hold nunca llegó a crearse.
 * @param cancelledBy el login de quien la canceló (el dueño o un administrador), `CATEDRA` si la
 * canceló la cátedra, o null si no está cancelada.
 */
data class AdminReservation(
    val reservation: Reservation,
    val owner: ReservationOwner,
    val reservationProcessId: String?,
    val cancelledBy: String?,
)

/** El dueño de una reserva. */
data class ReservationOwner(
    val login: String,
    val firstName: String,
    val lastName: String,
) {
    val fullName: String get() = "$firstName $lastName"
}
