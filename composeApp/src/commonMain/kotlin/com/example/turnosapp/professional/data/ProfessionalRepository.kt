package com.example.turnosapp.professional.data

import com.example.turnosapp.professional.domain.model.ProfessionalAgenda

/** Los datos vigentes de un profesional, del catálogo. */
interface ProfessionalRepository {

    /**
     * La agenda vigente. Un profesional inexistente llega como `AppError.Business`
     * (`PROFESSIONAL_NOT_FOUND`); uno no habilitado se devuelve igual, con `enabled` en falso.
     */
    suspend fun getAgenda(professionalId: Long): Result<ProfessionalAgenda>
}
