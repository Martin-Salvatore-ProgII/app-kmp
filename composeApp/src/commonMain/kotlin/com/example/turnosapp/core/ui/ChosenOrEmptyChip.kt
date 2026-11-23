package com.example.turnosapp.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Un filtro que se suma (ADR-0079), como chip. Lo usan Buscar y Admin. Sin valor, un chip que abre su
 * hoja. Con valor, un chip marcado que muestra lo elegido: tocarlo abre la hoja para cambiarlo, y la
 * cruz quita el filtro.
 */
@Composable
fun ChosenOrEmptyChip(
    chosenLabel: String?,
    emptyLabel: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    if (chosenLabel == null) {
        FilterChip(
            selected = false,
            onClick = onOpen,
            label = { Text(emptyLabel) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.heightIn(min = 40.dp),
        )
    } else {
        InputChip(
            selected = true,
            onClick = onOpen,
            label = { Text(chosenLabel) },
            trailingIcon = {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Quitar $emptyLabel",
                    modifier = Modifier.size(20.dp).clip(CircleShape).clickable(onClick = onRemove),
                )
            },
            modifier = Modifier.heightIn(min = 40.dp),
        )
    }
}
