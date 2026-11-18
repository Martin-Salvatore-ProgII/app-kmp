package com.example.turnosapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Un círculo con las iniciales de un profesional, en Buscar y en Profesional. Uno no habilitado
 * va en gris, para que se distinga sin leer el texto.
 */
@Composable
fun InitialsAvatar(
    firstName: String,
    lastName: String,
    enabled: Boolean,
    size: Dp = 44.dp,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) colors.secondaryContainer else colors.surfaceVariant),
    ) {
        Text(
            "${firstName.take(1)}${lastName.take(1)}",
            style = if (size > 50.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = if (enabled) colors.onSecondaryContainer else colors.onSurfaceVariant,
        )
    }
}
