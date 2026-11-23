package com.example.turnosapp.search.data.remote.dto

import kotlinx.serialization.Serializable

// Copian el contrato del catálogo (`docs/arq/contratos.md`, Servicio de catálogo). Solo los campos que usa la app.

/** Una categoría de `GET /api/professional-categories`. */
@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
)

/** Un profesional de `GET /api/professionals`. */
@Serializable
data class ProfessionalSummaryDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val category: CategoryRefDto,
    val enabled: Boolean,
)

@Serializable
data class CategoryRefDto(
    val id: Long,
    val name: String,
)

/** `GET /api/catalog/sync-status`. La app solo usa si hay una sincronización en curso (ADR-0079). */
@Serializable
data class SyncStatusDto(
    val syncInProgress: Boolean,
)
