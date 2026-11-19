package com.example.turnosapp.availability.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.availability.domain.FindAttendedDayUseCase
import com.example.turnosapp.availability.domain.model.Slot
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.AppDatePickerDialog
import com.example.turnosapp.core.ui.label
import com.example.turnosapp.core.ui.longLabel
import kotlinx.datetime.LocalDate

/** Conecta la pantalla con su ViewModel y traduce sus efectos en navegación. */
@Composable
fun AvailabilityRoute(
    container: AppContainer,
    professionalId: Long,
    date: LocalDate,
    onBack: () -> Unit,
    viewModel: AvailabilityViewModel = viewModel {
        AvailabilityViewModel(
            container.availabilityRepository,
            container.professionalRepository,
            FindAttendedDayUseCase(),
            professionalId,
            date,
        )
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AvailabilityEffect.NavigateBack -> onBack()
            }
        }
    }
    AvailabilityScreen(state = state, onAction = viewModel::onAction)
}

/** Turnos disponibles (prototipo en `docs/arq/prototipo/`). Solo dibuja el estado y emite acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(
    state: AvailabilityUiState,
    onAction: (AvailabilityAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            // El Scaffold de la navegación ya deja el lugar de la barra de estado: no se repite.
            windowInsets = WindowInsets(0),
            title = { Text("Turnos disponibles") },
            navigationIcon = {
                IconButton(onClick = { onAction(AvailabilityAction.BackClicked) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.professionalName.orEmpty(), style = MaterialTheme.typography.titleLarge)
                state.categoryName?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DayBar(state, onAction)
            DayContent(state, onAction)
        }
    }

    if (state.isPickingDate) {
        AppDatePickerDialog(
            initialDate = state.date,
            // Como en Profesional: días que no pasaron y en los que atiende (ADR-0079). Sin la agenda, cualquier día que no pasó.
            isSelectable = { it >= state.today && (state.attendedDays.isEmpty() || it.dayOfWeek in state.attendedDays) },
            onDateSelected = { onAction(AvailabilityAction.DateChosen(it)) },
            onDismiss = { onAction(AvailabilityAction.DatePickerDismissed) },
        )
    }
}

/** La fecha con las flechas al día anterior y siguiente en que atiende. Tocar la fecha abre el calendario. */
@Composable
private fun DayBar(state: AvailabilityUiState, onAction: (AvailabilityAction) -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = { onAction(AvailabilityAction.PreviousDayClicked) }, enabled = state.previousDay != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior que atiende")
            }
            TextButton(onClick = { onAction(AvailabilityAction.DateClicked) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    state.date.longLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            IconButton(onClick = { onAction(AvailabilityAction.NextDayClicked) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo día que atiende")
            }
        }
    }
}

@Composable
private fun DayContent(state: AvailabilityUiState, onAction: (AvailabilityAction) -> Unit) {
    val slots = state.slots
    val error = state.errorMessage
    when {
        state.isLoading -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.isDateOutOfRange -> EmptyState(
            icon = Icons.Filled.DateRange,
            title = "Todavía no se pueden pedir turnos para esta fecha",
            text = "Elegí un día más cercano.",
            buttonText = "Elegir otra fecha",
            onClick = { onAction(AvailabilityAction.DateClicked) },
        )
        error != null -> EmptyState(
            icon = Icons.Filled.Warning,
            title = "No pudimos cargar los turnos",
            text = error,
            buttonText = "Reintentar",
            onClick = { onAction(AvailabilityAction.Retry) },
        )
        slots == null -> Unit
        // Una lista vacía no es un error: no atiende ese día o ya no quedan libres (HU-06, criterio 6).
        slots.isEmpty() -> EmptyState(
            icon = Icons.Filled.DateRange,
            title = "No quedan turnos libres este día",
            text = "Probá con el próximo día que atiende.",
            buttonText = "Ver ${state.nextDay.longLabel().replaceFirstChar { it.lowercase() }}",
            onClick = { onAction(AvailabilityAction.NextDayClicked) },
        )
        else -> SlotGrid(slots)
    }
}

@Composable
private fun SlotGrid(slots: List<Slot>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val count = if (slots.size == 1) "1 turno libre" else "${slots.size} turnos libres"
        Text(
            "$count · ${slots.first().durationMinutes} min",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Tres por fila: botones grandes, cómodos para el dedo.
        slots.chunked(SLOTS_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { slot ->
                    SlotBox(slot, Modifier.weight(1f))
                }
                repeat(SLOTS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        // HU-06, criterio 8: ver un turno libre no garantiza la reserva.
        Text(
            "Un turno libre no está garantizado hasta que la reserva se confirma.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SlotBox(slot: Slot, modifier: Modifier = Modifier) {
    OutlinedCard(shape = RoundedCornerShape(12.dp), modifier = modifier) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
            Text(slot.startTime.label(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Un mensaje centrado con un ícono y una acción: sin turnos, fecha fuera de rango o error. */
@Composable
private fun EmptyState(icon: ImageVector, title: String, text: String, buttonText: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, start = 8.dp, end = 8.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        OutlinedButton(onClick = onClick, modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp)) { Text(buttonText) }
    }
}

private const val SLOTS_PER_ROW = 3
