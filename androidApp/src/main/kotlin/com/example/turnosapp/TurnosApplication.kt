package com.example.turnosapp

import android.app.Application
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.network.ServiceUrls
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Vive lo mismo que el proceso de la app. Acá se crea el contenedor de dependencias, una sola vez:
 * si lo creara la Activity, se volvería a crear en cada rotación de pantalla (ADR-0052).
 *
 * Es el punto donde lo de Android entra al código común: el motor HTTP, las URL del build y el
 * almacenamiento cifrado del JWT.
 */
class TurnosApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            // OkHttp respeta la configuración de red de Android, donde se confía en el certificado local (ADR-0075).
            httpEngine = OkHttp.create(),
            urls = ServiceUrls(
                turnos = BuildConfig.TURNOS_URL,
                catalog = BuildConfig.CATALOG_URL,
            ),
            tokenStorage = KeystoreTokenStorage(this),
        )
    }
}
