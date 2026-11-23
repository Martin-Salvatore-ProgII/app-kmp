package com.example.turnosapp.core.error

/**
 * Los errores que entiende la app. Toda falla de red o respuesta de error de un backend llega a los
 * ViewModels como uno de estos, traducida una sola vez en `core/network` (ADR-0051).
 * Ningún ViewModel ve una excepción de Ktor ni el texto de `detail`.
 */
sealed class AppError : Exception() {

    /** 401: sin sesión o con el JWT vencido. La app borra el JWT y vuelve a Login. */
    data object Unauthorized : AppError()

    /** 403: la sesión es válida pero no tiene permiso para la operación. */
    data object Forbidden : AppError()

    /** 400 con `fieldErrors`: campo del formulario → mensaje del backend. */
    data class Validation(val fieldErrors: Map<String, String>) : AppError()

    /** 4xx con `code` funcional (por ejemplo, 409 `ACTIVE_RESERVATION_EXISTS`). */
    data class Business(val status: Int, val code: String) : AppError()

    /** 503: el servicio no está disponible por ahora; se puede reintentar. */
    data object Unavailable : AppError()

    /**
     * 503 `CATALOG_NOT_READY`: el catálogo todavía no tiene su primera copia. Es el único 503 que la
     * app distingue, porque no es una falla sino una espera: Buscar lo muestra con su propia pantalla
     * (ADR-0079).
     */
    data object CatalogNotReady : AppError()

    /** No se llegó al servidor: sin conexión, timeout o certificado no aceptado. */
    data object Network : AppError()

    /** Cualquier otra cosa (500, una respuesta que no se entiende). */
    data object Unknown : AppError()
}
