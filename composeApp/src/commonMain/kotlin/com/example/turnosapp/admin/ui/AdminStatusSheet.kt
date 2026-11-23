package com.example.turnosapp.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.turnosapp.reservation.domain.model.ReservationStatus

/**
 * La hoja para elegir el estado (prototipo "Admin, elegir estado"): los diez estados, separados en
 * los que siguen en curso y los terminados. Elegir uno aplica el filtro y cierra la hoja.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatusSheet(selected: ReservationStatus?, onAction: (AdminAction) -> Unit) {
    val (inProgress, finished) = ReservationStatus.entries.partition { !it.isFinal }
    ModalBottomSheet(
        onDismissRequest = { onAction(AdminAction.PickerDismissed) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text("Estado", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Option("Todos los estados", selected = selected == null) { onAction(AdminAction.StatusSelected(null)) }
                GroupTitle("En curso")
                inProgress.forEach { status ->
                    Option(status.adminLabel(), selected = status == selected) { onAction(AdminAction.StatusSelected(status)) }
                }
                GroupTitle("Terminadas")
                finished.forEach { status ->
                    Option(status.adminLabel(), selected = status == selected) { onAction(AdminAction.StatusSelected(status)) }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Option(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Elegido")
    }
}
