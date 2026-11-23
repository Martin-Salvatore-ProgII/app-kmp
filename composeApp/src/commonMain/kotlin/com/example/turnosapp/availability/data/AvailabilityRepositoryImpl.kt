package com.example.turnosapp.availability.data

import com.example.turnosapp.availability.data.mapper.AvailabilityMapper
import com.example.turnosapp.availability.data.remote.AvailabilityApi
import com.example.turnosapp.availability.domain.model.Availability
import com.example.turnosapp.core.network.runCatchingApi
import kotlinx.datetime.LocalDate

class AvailabilityRepositoryImpl(
    private val api: AvailabilityApi,
    private val mapper: AvailabilityMapper,
) : AvailabilityRepository {

    override suspend fun getAvailability(professionalId: Long, date: LocalDate): Result<Availability> =
        runCatchingApi { mapper.toDomain(api.getAvailability(professionalId, date)) }
}
