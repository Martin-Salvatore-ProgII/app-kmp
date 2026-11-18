package com.example.turnosapp.core.network

/**
 * URL base de los dos backends con los que habla la app (ADR-0001). Nunca de la cátedra.
 * Los valores salen de la configuración del build de androidApp (ADR-0075), no de este código.
 */
data class ServiceUrls(
    val turnos: String,
    val catalog: String,
)
