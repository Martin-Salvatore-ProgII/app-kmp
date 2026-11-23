package com.example.turnosapp.core.model

/**
 * Una página de resultados y el total que informa el backend en `X-Total-Count`
 * (`docs/arq/contratos.md`, convenciones). La paginación se arma a mano, sin librería (ADR-0052).
 */
data class Page<T>(
    val items: List<T>,
    val totalCount: Int,
)
