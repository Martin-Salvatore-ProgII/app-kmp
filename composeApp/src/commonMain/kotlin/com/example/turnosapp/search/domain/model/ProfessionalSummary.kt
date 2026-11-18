package com.example.turnosapp.search.domain.model

/**
 * Un profesional en la lista de resultados.
 *
 * @param enabled el habilitado efectivo que resuelve el catálogo (ADR-0014): si es falso, el
 * profesional se muestra como no disponible para reservar.
 */
data class ProfessionalSummary(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val categoryName: String,
    val enabled: Boolean,
) {
    val fullName: String get() = "$firstName $lastName"
}
