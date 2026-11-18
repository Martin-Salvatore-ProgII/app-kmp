package com.example.turnosapp.core.ui

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * El botón para ver u ocultar la contraseña, en Login y en Registro. Es texto y no un ojo porque
 * el set de íconos de la app (material-icons-core) no lo trae, y sumar el extendido por un solo
 * ícono no se justifica.
 */
@Composable
fun PasswordVisibilityButton(visible: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(if (visible) "Ocultar" else "Mostrar")
    }
}
