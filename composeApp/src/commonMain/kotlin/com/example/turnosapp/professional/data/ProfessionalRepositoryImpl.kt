package com.example.turnosapp.professional.data

import com.example.turnosapp.core.network.runCatchingApi
import com.example.turnosapp.professional.data.mapper.ProfessionalMapper
import com.example.turnosapp.professional.data.remote.ProfessionalApi
import com.example.turnosapp.professional.domain.model.ProfessionalAgenda

class ProfessionalRepositoryImpl(
    private val api: ProfessionalApi,
    private val mapper: ProfessionalMapper,
) : ProfessionalRepository {

    override suspend fun getAgenda(professionalId: Long): Result<ProfessionalAgenda> =
        runCatchingApi { mapper.toDomain(api.getAgenda(professionalId)) }
}
