package com.example.turnosapp.core.di

import com.example.turnosapp.auth.data.AuthRepository
import com.example.turnosapp.availability.data.AvailabilityRepository
import com.example.turnosapp.availability.data.AvailabilityRepositoryImpl
import com.example.turnosapp.availability.data.mapper.AvailabilityMapper
import com.example.turnosapp.availability.data.remote.AvailabilityApi
import com.example.turnosapp.auth.data.AuthRepositoryImpl
import com.example.turnosapp.auth.data.mapper.RegistrationMapper
import com.example.turnosapp.auth.data.remote.AuthApi
import com.example.turnosapp.core.network.ServiceUrls
import com.example.turnosapp.core.network.createHttpClient
import com.example.turnosapp.core.session.SessionRepository
import com.example.turnosapp.core.session.SessionRepositoryImpl
import com.example.turnosapp.core.session.TokenStorage
import com.example.turnosapp.professional.data.ProfessionalRepository
import com.example.turnosapp.professional.data.ProfessionalRepositoryImpl
import com.example.turnosapp.professional.data.mapper.ProfessionalMapper
import com.example.turnosapp.professional.data.remote.ProfessionalApi
import com.example.turnosapp.reservation.data.ReservationRepository
import com.example.turnosapp.reservation.data.ReservationRepositoryImpl
import com.example.turnosapp.reservation.data.mapper.ReservationMapper
import com.example.turnosapp.reservation.data.remote.ReservationApi
import com.example.turnosapp.search.data.SearchRepository
import com.example.turnosapp.search.data.SearchRepositoryImpl
import com.example.turnosapp.search.data.mapper.SearchMapper
import com.example.turnosapp.search.data.remote.SearchApi
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

    val searchRepository: SearchRepository = SearchRepositoryImpl(SearchApi(httpClient, urls), SearchMapper())

    val professionalRepository: ProfessionalRepository =
        ProfessionalRepositoryImpl(ProfessionalApi(httpClient, urls), ProfessionalMapper())

    val availabilityRepository: AvailabilityRepository =
        AvailabilityRepositoryImpl(AvailabilityApi(httpClient, urls), AvailabilityMapper())

    val reservationRepository: ReservationRepository =
        ReservationRepositoryImpl(ReservationApi(httpClient, urls), ReservationMapper())
}
