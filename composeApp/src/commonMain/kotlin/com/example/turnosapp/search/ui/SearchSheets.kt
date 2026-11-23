package com.example.turnosapp.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.turnosapp.core.ui.AppTimePickerDialog
import com.example.turnosapp.core.ui.label
import com.example.turnosapp.core.ui.longLabel
import com.example.turnosapp.search.domain.model.DayPeriod
import com.example.turnosapp.search.domain.model.TimeRange
import kotlinx.datetime.LocalTime

// Las hojas inferiores de los filtros (prototipo: "elegir categoría" y "elegir franja").
// Cada opción es una fila alta, cómoda para el dedo, y elegirla aplica el filtro y cierra la hoja.

@Composable
internal fun CategorySheet(state: SearchUiState, onAction: (SearchAction) -> Unit) {
    FilterSheet(title = "Categoría", onAction = onAction) {
        Option("Todas", selected = state.filters.category == null) { onAction(SearchAction.CategorySelected(null)) }
        state.categories.forEach { category ->
            Option(category.name, selected = state.filters.category == category) {
                onAction(SearchAction.CategorySelected(category))
            }
        }
    }
}

@Composable
internal fun EnabledSheet(state: SearchUiState, onAction: (SearchAction) -> Unit) {
    FilterSheet(title = "Estado", onAction = onAction) {
        Option("Habilitados", selected = state.filters.enabled) { onAction(SearchAction.EnabledSelected(true)) }
        Option("No habilitados", detail = "no se pueden reservar", selected = !state.filters.enabled) {
            onAction(SearchAction.EnabledSelected(false))
        }
    }
}

@Composable
internal fun TimeRangeSheet(state: SearchUiState, onAction: (SearchAction) -> Unit) {
    val current = state.filters.timeRange
    val draft = state.customRange
    val isCustom = current != null && DayPeriod.entries.none { it.range == current }
    FilterSheet(title = "Franja horaria", subtitle = state.filters.date?.longLabel(), onAction = onAction) {
        Option("Cualquier hora", selected = current == null && !draft.expanded) {
            onAction(SearchAction.DayPeriodSelected(null))
        }
        DayPeriod.entries.forEach { period ->
            Option(period.label(), detail = period.detail(), selected = current == period.range && !draft.expanded) {
                onAction(SearchAction.DayPeriodSelected(period))
            }
        }
        Option("Personalizada", selected = draft.expanded || isCustom) {
            onAction(SearchAction.CustomRangeExpanded)
        }
        if (draft.expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                TimeButton("Desde", draft.from) { onAction(SearchAction.CustomTimeEditing(TimeField.FROM)) }
                TimeButton("Hasta", draft.to) { onAction(SearchAction.CustomTimeEditing(TimeField.TO)) }
                draft.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = { onAction(SearchAction.CustomRangeApplied) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Aplicar") }
            }
        }
    }
}

/** El reloj de la franja personalizada, abierto sobre la hoja mientras se elige una hora. */
@Composable
internal fun CustomTimePicker(state: SearchUiState, onAction: (SearchAction) -> Unit) {
    val field = state.customRange.editing ?: return
    AppTimePickerDialog(
        title = if (field == TimeField.FROM) "Desde" else "Hasta",
        initialTime = if (field == TimeField.FROM) state.customRange.from else state.customRange.to,
        onTimeSelected = { onAction(SearchAction.CustomTimePicked(it)) },
        onDismiss = { onAction(SearchAction.CustomTimeEditing(null)) },
    )
}

/** "Noche" para una franja fija, o "10:00 a 11:00" para una personalizada. */
internal fun timeRangeLabel(range: TimeRange): String =
    DayPeriod.entries.firstOrNull { it.range == range }?.label() ?: "${range.from.label()} a ${range.to.label()}"

private fun DayPeriod.label(): String = when (this) {
    DayPeriod.MORNING -> "Mañana"
    DayPeriod.AFTERNOON -> "Tarde"
    DayPeriod.EVENING -> "Noche"
}

private fun DayPeriod.detail(): String = when (this) {
    DayPeriod.MORNING -> "hasta las 12"
    DayPeriod.AFTERNOON -> "de 12 a 18"
    DayPeriod.EVENING -> "desde las 18"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    title: String,
    onAction: (SearchAction) -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(SearchAction.SheetDismissed) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                )
            }
            Column(modifier = Modifier.padding(top = 8.dp), content = content)
        }
    }
}

@Composable
private fun Option(text: String, selected: Boolean, detail: String? = null, onClick: () -> Unit) {
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
        detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant) }
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Elegido")
    }
}

@Composable
private fun TimeButton(label: String, time: LocalTime?, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Text(
            if (time == null) "$label: elegir hora" else "$label: ${time.label()}",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
