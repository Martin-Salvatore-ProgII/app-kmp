package com.example.turnosapp.core.network

import com.example.turnosapp.core.session.SessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Valores de la tabla de RNF-09 (ADR-0075).
private const val CONNECT_TIMEOUT_MILLIS = 5_000L
private const val REQUEST_TIMEOUT_MILLIS = 60_000L

/** JSON de los dos backends. Ignora campos desconocidos: un campo nuevo del backend no rompe la app. */
internal val appJson = Json {
    ignoreUnknownKeys = true
}

/**
 * Crea el único cliente HTTP de la app, compartido por las APIs de los dos servicios.
 *
 * El motor llega de afuera (lo crea androidApp con OkHttp) para que la confianza en el
 * certificado local la resuelva la configuración de red de Android (ADR-0075).
 * La sesión le da el JWT de cada pedido y se entera de los 401 ([sessionPlugin]).
 */
fun createHttpClient(engine: HttpClientEngine, sessionRepository: SessionRepository): HttpClient =
    HttpClient(engine) {
        // Una respuesta 4xx o 5xx se lanza como excepción, y runCatchingApi la traduce a AppError.
        expectSuccess = true

        install(ContentNegotiation) {
            json(appJson)
        }
        install(sessionPlugin(sessionRepository))
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    }
