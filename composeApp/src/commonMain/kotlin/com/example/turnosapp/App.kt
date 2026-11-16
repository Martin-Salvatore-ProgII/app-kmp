package com.example.turnosapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.theme.AppTheme

/**
 * Composable raíz de la app: aplica el tema y recibe el contenedor de dependencias,
 * que después le pasa a cada pantalla. La navegación se suma en la issue siguiente.
 */
@Composable
fun App(container: AppContainer) {
    AppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                Text("Turnos")
            }
        }
    }
}
