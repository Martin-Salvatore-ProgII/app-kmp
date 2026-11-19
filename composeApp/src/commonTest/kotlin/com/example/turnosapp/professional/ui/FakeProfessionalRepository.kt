package com.example.turnosapp.professional.ui

import com.example.turnosapp.professional.data.ProfessionalRepository
import com.example.turnosapp.professional.domain.model.ProfessionalAgenda

/** Devuelve la agenda que el escenario necesita. La usan Profesional y Turnos disponibles. */
class FakeProfessionalRepository(var result: Result<ProfessionalAgenda>) : ProfessionalRepository {
    override suspend fun getAgenda(professionalId: Long): Result<ProfessionalAgenda> = result
}
