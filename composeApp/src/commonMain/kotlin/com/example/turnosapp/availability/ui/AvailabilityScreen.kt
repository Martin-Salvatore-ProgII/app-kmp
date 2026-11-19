package com.example.turnosapp.availability.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.turnosapp.core.ui.PlaceholderScreen
import com.example.turnosapp.core.ui.longLabel
import kotlinx.datetime.LocalDate

/** Turnos disponibles (HU-06). Provisoria hasta la M4: muestra a qué profesional y fecha se llegó. */
@Composable
fun AvailabilityRoute(professionalId: Long, date: LocalDate) {
    PlaceholderScreen("Turnos disponibles") {
        Text("Profesional $professionalId · ${date.longLabel()}")
    }
}
