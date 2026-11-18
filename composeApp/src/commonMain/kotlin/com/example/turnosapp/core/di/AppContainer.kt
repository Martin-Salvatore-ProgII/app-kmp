package com.example.turnosapp.core.di

import com.example.turnosapp.auth.data.AuthRepository
import com.example.turnosapp.auth.data.AuthRepositoryImpl
import com.example.turnosapp.auth.data.mapper.RegistrationMapper
import com.example.turnosapp.auth.data.remote.AuthApi
import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.createHttpClient
import com.example.turnosapp.core.session.SessionRepository
import com.example.turnosapp.core.session.SessionRepositoryImpl
import com.example.turnosapp.core.session.TokenStorage
import io.ktor.client.engine.HttpClientEngine

/**
 * Crea a mano, una sola vez, los objetos que comparte toda la app (ADR-0052): el cliente HTTP,
 * los repositorios y las implementaciones de plataforma que le pasa androidApp.
 *
 * Los ViewModels no van acá: cada pantalla crea el suyo con los repositorios de este contenedor,
 * así viven y mueren con su pantalla. Cada repositorio nuevo suma una propiedad a esta clase.
 *
 * @param httpEngine motor HTTP de la plataforma (OkHttp en Android, ADR-0075).
 * @param urls URL de los dos backends, de la configuración del build.
 * @param tokenStorage almacenamiento cifrado del JWT, implementado en androidApp (ADR-0049).
 */
class AppContainer(
    httpEngine: HttpClientEngine,
    private val urls: ServiceUrls,
    tokenStorage: TokenStorage,
) {
    val sessionRepository: SessionRepository = SessionRepositoryImpl(tokenStorage)

    // Las APIs de cada feature lo reciben junto con `urls` (por ejemplo, `ReservationApi(httpClient, urls)`).
    private val httpClient = createHttpClient(httpEngine, sessionRepository)

    val authRepository: AuthRepository = AuthRepositoryImpl(AuthApi(httpClient, urls), sessionRepository, RegistrationMapper())
}
