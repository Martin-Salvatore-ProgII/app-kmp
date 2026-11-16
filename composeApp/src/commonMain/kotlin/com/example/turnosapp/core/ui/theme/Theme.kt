package com.example.turnosapp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Teal40,
    primaryContainer = Teal90,
    secondary = Slate40,
    tertiary = Indigo40,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    secondary = Slate80,
    tertiary = Indigo80,
)

/**
 * Tema de Material 3 de la app (ADR-0048), con modo claro y oscuro según el sistema.
 * No usa los colores dinámicos de Android 12+: dependen de la plataforma, y la app
 * mantiene la misma identidad en cualquier dispositivo.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
