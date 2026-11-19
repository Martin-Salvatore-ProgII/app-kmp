package com.example.turnosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Punto de entrada de Android. Muestra la interfaz común, que vive en el módulo composeApp,
 * con el contenedor de dependencias que creó TurnosApplication.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as TurnosApplication).container
        setContent {
            App(container)
        }
    }
}
