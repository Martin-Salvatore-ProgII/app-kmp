package com.example.turnosapp.admin.ui

import com.example.turnosapp.admin.data.AdminReservationRepository
import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.admin.domain.model.AdminReservationFilters
import com.example.turnosapp.admin.domain.model.ReservationOwner
import com.example.turnosapp.core.model.Page
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import com.example.turnosapp.reservation.ui.reservation
import kotlinx.datetime.LocalDate

/** Devuelve lo que el escenario necesita y anota los pedidos. */
open class FakeAdminReservationRepository : AdminReservationRepository {

    /** Las respuestas del listado, por página pedida; una página sin respuesta llega vacía. */
    val listResults = mutableMapOf<Int, Result<Page<AdminReservation>>>()
    val listCalls = mutableListOf<Pair<AdminReservationFilters, Int>>()

    override suspend fun list(filters: AdminReservationFilters, page: Int): Result<Page<AdminReservation>> {
        listCalls += filters to page
        return listResults[page] ?: Result.success(Page(emptyList(), 0))
    }

    var getResult: Result<AdminReservation> = Result.success(adminReservation(ReservationStatus.CONFIRMED))
    var getCalls = 0

    override suspend fun get(reservationId: String): Result<AdminReservation> {
        getCalls++
        return getResult
    }

    var cancelResult: Result<AdminReservation> =
        Result.success(adminReservation(ReservationStatus.CANCELLED, cancelledBy = "admin"))
    val cancelReasons = mutableListOf<String>()

    override suspend fun cancel(reservationId: String, reason: String): Result<AdminReservation> {
        cancelReasons += reason
        return cancelResult
    }
}

/** Una reserva vista por el administrador, con un dueño de prueba. */
fun adminReservation(
    status: ReservationStatus,
    id: String = "5b1f",
    date: LocalDate = LocalDate(2026, 11, 26),
    failureReason: String? = null,
    ownerLogin: String = "jperez",
    reservationProcessId: String? = "8c2f6a1e",
    cancelledBy: String? = null,
) = AdminReservation(
    reservation = reservation(status, id = id, date = date, failureReason = failureReason),
    owner = ReservationOwner(ownerLogin, "Juan", "Pérez"),
    reservationProcessId = reservationProcessId,
    cancelledBy = cancelledBy,
)
