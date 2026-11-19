package com.example.turnosapp.availability.data

import com.example.turnosapp.availability.domain.model.Availability
import kotlinx.datetime.LocalDate

/** Los turnos libres, de turnos, que los arma con la agenda vigente y las ocupaciones (CU-03). */
interface AvailabilityRepository {

    /**
     * Los errores llegan como `AppError`: `DATE_OUT_OF_RANGE`, `PROFESSIONAL_DISABLED` y
     * `PROFESSIONAL_NOT_FOUND` como `Business`, y los 503 como `Unavailable`.
     */
    suspend fun getAvailability(professionalId: Long, date: LocalDate): Result<Availability>
}
