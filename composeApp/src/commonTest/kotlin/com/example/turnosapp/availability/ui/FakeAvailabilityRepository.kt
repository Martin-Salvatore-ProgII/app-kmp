package com.example.turnosapp.availability.ui

import com.example.turnosapp.availability.data.AvailabilityRepository
import com.example.turnosapp.availability.domain.model.Availability
import kotlinx.datetime.LocalDate

/** Devuelve lo que el escenario necesita y recuerda qué fechas se pidieron. */
class FakeAvailabilityRepository(var result: (LocalDate) -> Result<Availability>) : AvailabilityRepository {
    val requestedDates = mutableListOf<LocalDate>()

    override suspend fun getAvailability(professionalId: Long, date: LocalDate): Result<Availability> {
        requestedDates += date
        return result(date)
    }
}
