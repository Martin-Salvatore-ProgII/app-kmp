package com.example.turnosapp

import androidx.compose.runtime.Composable
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.navigation.AppNavigation
import com.example.turnosapp.core.ui.theme.AppTheme

/** Composable raíz de la app: aplica el tema y arranca la navegación con el contenedor de dependencias. */
@Composable
fun App(container: AppContainer) {
    AppTheme {
        AppNavigation(container)
    }
}
