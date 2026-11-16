package com.example.turnosapp.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Las secciones de la barra inferior (`docs/arq/interfaz.md`, Navegación). */
enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val adminOnly: Boolean,
) {
    SEARCH(Search, "Buscar", Icons.Filled.Search, adminOnly = false),
    MY_RESERVATIONS(MyReservations, "Mis reservas", Icons.Filled.DateRange, adminOnly = false),

    // Solo visible con ROLE_ADMIN (ADR-0037). Es una decisión de interfaz: la autorización la hace el backend.
    ADMIN(Admin, "Admin", Icons.Filled.Settings, adminOnly = true),
}
