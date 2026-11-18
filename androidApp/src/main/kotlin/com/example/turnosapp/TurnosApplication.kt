package com.example.turnosapp

import android.app.Application
import com.example.turnosapp.core.di.AppContainer

/**
 * Vive lo mismo que el proceso de la app. Acá se crea el contenedor de dependencias, una sola vez:
 * si lo creara la Activity, se volvería a crear en cada rotación de pantalla (ADR-0052).
 */
class TurnosApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
