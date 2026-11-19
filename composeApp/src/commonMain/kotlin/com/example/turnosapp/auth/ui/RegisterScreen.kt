package com.example.turnosapp.auth.ui

import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.turnosapp.core.ui.PlaceholderScreen

/** Registro (HU-01). Provisoria hasta la M2. */
@Composable
fun RegisterRoute(onBack: () -> Unit) {
    PlaceholderScreen("Crear cuenta") {
        TextButton(onClick = onBack) { Text("Volver") }
    }
}
