package com.example.turnosapp.admin.ui

import com.example.turnosapp.admin.data.AdminReservationRepository
import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.admin.domain.model.AdminReservationFilters
import com.example.turnosapp.admin.domain.model.ReservationOwner
import com.example.turnosapp.core.model.Page
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import com.example.turnosapp.reservation.ui.reservation

/** Devuelve lo que el escenario necesita y anota los pedidos. */
open class FakeAdminReservationRepository : AdminReservationRepository {

    /** Las respuestas del listado, por página pedida; una página sin respuesta llega vacía. */
    val listResults = mutableMapOf<Int, Result<Page<AdminReservation>>>()
    val listCalls = mutableListOf<Pair<AdminReservationFilters, Int>>()

    override suspend fun list(filters: AdminReservationFilters, page: Int): Result<Page<AdminReservation>> {
        listCalls += filters to page
        return listResults[page] ?: Result.success(Page(emptyList(), 0))
    }
}

/** Una reserva vista por el administrador, con un dueño de prueba. */
fun adminReservation(
    status: ReservationStatus,
    id: String = "5b1f",
    ownerLogin: String = "jperez",
    reservationProcessId: String? = "8c2f6a1e",
    cancelledBy: String? = null,
) = AdminReservation(
    reservation = reservation(status, id = id),
    owner = ReservationOwner(ownerLogin, "Juan", "Pérez"),
    reservationProcessId = reservationProcessId,
    cancelledBy = cancelledBy,
)
