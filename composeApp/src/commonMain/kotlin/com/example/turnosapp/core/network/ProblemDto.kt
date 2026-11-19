package com.example.turnosapp.core.network

import kotlinx.serialization.Serializable

/**
 * Cuerpo `application/problem+json` de los dos backends (`docs/arq/contratos.md`, esquema `Problem`).
 * Solo los campos con los que decide la app. `detail` no se lee: nunca se decide por el texto.
 */
@Serializable
internal data class ProblemDto(
    val code: String? = null,
    val fieldErrors: List<FieldErrorDto> = emptyList(),
)

@Serializable
internal data class FieldErrorDto(
    val field: String,
    val message: String,
)
