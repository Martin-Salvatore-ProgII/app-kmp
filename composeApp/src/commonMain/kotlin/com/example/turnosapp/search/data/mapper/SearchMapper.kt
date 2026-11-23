package com.example.turnosapp.search.data.mapper

import com.example.turnosapp.search.data.remote.dto.CategoryDto
import com.example.turnosapp.search.data.remote.dto.ProfessionalSummaryDto
import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.ProfessionalSummary

/** Pasa las respuestas del catálogo a los modelos que usa la pantalla. */
class SearchMapper {

    fun toDomain(dto: CategoryDto): Category = Category(id = dto.id, name = dto.name)

    fun toDomain(dto: ProfessionalSummaryDto): ProfessionalSummary =
        ProfessionalSummary(
            id = dto.id,
            firstName = dto.firstName,
            lastName = dto.lastName,
            categoryName = dto.category.name,
            enabled = dto.enabled,
        )
}
