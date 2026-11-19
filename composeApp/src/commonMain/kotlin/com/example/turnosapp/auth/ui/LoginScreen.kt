package com.example.turnosapp.auth.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.turnosapp.core.ui.PlaceholderScreen

/** Inicio de sesión (HU-02). Provisoria hasta la M2: el botón entra sin pedir credenciales. */
@Composable
fun LoginRoute(
    onLoggedIn: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    PlaceholderScreen("Iniciar sesión") {
        Button(onClick = onLoggedIn) { Text("Entrar") }
        TextButton(onClick = onRegisterClick) { Text("Crear cuenta") }
    }
}
