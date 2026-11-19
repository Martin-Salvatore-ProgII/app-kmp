package com.example.turnosapp.reservation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.label
import com.example.turnosapp.core.ui.longLabel
import com.example.turnosapp.reservation.domain.model.Reservation
import kotlinx.datetime.LocalDate

/**
 * Conecta la pantalla con su ViewModel y traduce sus efectos en navegación. Le avisa al ViewModel
 * cuándo la pantalla se ve y cuándo no, para consultar solo mientras se ve (ADR-0080).
 */
@Composable
fun ReservationRoute(
    container: AppContainer,
    reservationId: String,
    onBack: () -> Unit,
    onMyReservations: () -> Unit,
    onSearch: () -> Unit,
    onChooseAnotherSlot: (professionalId: Long, date: LocalDate) -> Unit,
    viewModel: ReservationViewModel = viewModel { ReservationViewModel(container.reservationRepository, reservationId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.onAction(ReservationAction.ScreenStarted)
        onStopOrDispose { viewModel.onAction(ReservationAction.ScreenStopped) }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReservationEffect.NavigateBack -> onBack()
                ReservationEffect.NavigateToMyReservations -> onMyReservations()
                ReservationEffect.NavigateToSearch -> onSearch()
                is ReservationEffect.NavigateToAvailability -> onChooseAnotherSlot(effect.professionalId, effect.date)
            }
        }
    }
    ReservationScreen(state = state, onAction = viewModel::onAction)
}

/** Reserva en curso (prototipos en `docs/arq/prototipo/`). Solo dibuja el estado y emite acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationScreen(
    state: ReservationUiState,
    onAction: (ReservationAction) -> Unit,
) {
    val phase = state.phase
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            // El Scaffold de la navegación ya deja el lugar de la barra de estado: no se repite.
            windowInsets = WindowInsets(0),
            title = { Text(if (phase.isFinal) "Reserva" else "Reserva en curso") },
            navigationIcon = {
                IconButton(onClick = { onAction(ReservationAction.BackClicked) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
        )
        val reservation = state.reservation
        val error = state.errorMessage
        when {
            reservation != null -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                ) {
                    if (state.isConnectionLost) ConnectionNotice()
                    if (phase.isFinal) FinalContent(phase, reservation) else InProgressContent(state, reservation)
                }
                if (phase.isFinal) FinalButtons(phase, onAction)
            }
            error != null -> CenteredMessage(error) {
                Button(onClick = { onAction(ReservationAction.SearchClicked) }) { Text("Volver a buscar") }
            }
            else -> CenteredMessage(null) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun InProgressContent(state: ReservationUiState, reservation: Reservation) {
    Summary(reservation)
    Steps(state.phase.step)
    when (state.phase) {
        ReservationPhase.Reserving -> Waiting(
            "Reservando tu turno…",
            "Apartamos el horario para vos. En unos segundos te pedimos un dato para confirmar.",
        )
        ReservationPhase.AwaitingPhone -> {
            reservation.message?.let { CatedraMessage(it) }
            Countdown(state.remainingSeconds, state.totalSeconds)
        }
        ReservationPhase.TimeUp -> {
            Countdown(0, state.totalSeconds)
            Waiting("Se terminó el tiempo", "Estamos esperando la respuesta final. Puede tardar un minuto.")
        }
        ReservationPhase.Confirming -> Waiting("Confirmando tu reserva…", "Recibimos tu teléfono. Falta la confirmación final.")
        else -> Unit
    }
}

@Composable
private fun FinalContent(phase: ReservationPhase, reservation: Reservation) {
    val scheme = MaterialTheme.colorScheme
    when (phase) {
        ReservationPhase.Confirmed -> Result(
            Icons.Filled.Check, scheme.primary, scheme.onPrimary,
            "¡Turno reservado!",
            "Te esperamos el ${reservation.date.longLabel().replaceFirstChar { it.lowercase() }} a las ${reservation.startTime.label()}.",
        )
        ReservationPhase.Cancelled -> Result(
            Icons.Filled.Info, scheme.surfaceVariant, scheme.onSurfaceVariant,
            "La reserva fue cancelada",
            "El turno quedó libre.",
        )
        ReservationPhase.Expired -> Result(
            Icons.Filled.Info, scheme.surfaceVariant, scheme.onSurfaceVariant,
            "El tiempo para completar la reserva venció",
            "El turno quedó libre. Podés elegir otro horario.",
        )
        is ReservationPhase.Failed -> Result(
            Icons.Filled.Warning, scheme.errorContainer, scheme.onErrorContainer,
            "No se pudo completar la reserva",
            phase.message,
        )
        else -> Unit
    }
    Summary(reservation)
    if (phase == ReservationPhase.Confirmed) Steps(phase.step)
}

/** Confirmada: ir a Mis reservas. Vencida o fallida: elegir otro turno del mismo día (ADR-0080). */
@Composable
private fun FinalButtons(phase: ReservationPhase, onAction: (ReservationAction) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
    ) {
        val reserved = phase == ReservationPhase.Confirmed || phase == ReservationPhase.Cancelled
        Button(
            onClick = { onAction(if (reserved) ReservationAction.MyReservationsClicked else ReservationAction.ChooseAnotherSlotClicked) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(if (reserved) "Ver mis reservas" else "Elegir otro turno") }
        TextButton(onClick = { onAction(ReservationAction.SearchClicked) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Volver a buscar")
        }
    }
}

@Composable
private fun Summary(reservation: Reservation) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(16.dp)) {
            Text(reservation.professionalFullName, style = MaterialTheme.typography.titleMedium)
            Text(
                "${reservation.date.longLabel()} · ${reservation.startTime.label()} a ${reservation.endTime.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** El indicador de pasos: Reservado, Teléfono, Confirmado. */
@Composable
private fun Steps(current: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Reservado", "Teléfono", "Confirmado").forEachIndexed { index, name ->
            val number = index + 1
            if (index > 0) HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepCircle(number, done = number < current, isCurrent = number == current)
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (number == current) FontWeight.Medium else FontWeight.Normal,
                    color = if (number == current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StepCircle(number: Int, done: Boolean, isCurrent: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when {
        done -> scheme.primary to scheme.onPrimary
        isCurrent -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> Color.Transparent to scheme.onSurfaceVariant
    }
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (done || isCurrent) null else androidx.compose.foundation.BorderStroke(1.dp, scheme.outline),
        modifier = Modifier.size(24.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (done) Icon(Icons.Filled.Check, contentDescription = "Hecho", modifier = Modifier.size(14.dp))
            else Text("$number", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** El mensaje de la cátedra, tal cual llega (`interfaz.md`). */
@Composable
private fun CatedraMessage(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
    }
}

/** "Tiempo para completar 4:32" y una barra que se vacía. */
@Composable
private fun Countdown(remainingSeconds: Long?, totalSeconds: Long?) {
    if (remainingSeconds == null) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text(
                "Tiempo para completar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        val total = totalSeconds?.takeIf { it > 0 } ?: remainingSeconds.coerceAtLeast(1)
        LinearProgressIndicator(
            progress = { remainingSeconds.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
            drawStopIndicator = {},
        )
    }
}

/** Algo que la app está esperando: el pedido de teléfono, la confirmación o el resultado. */
@Composable
private fun Waiting(title: String, text: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Result(icon: ImageVector, container: Color, content: Color, title: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 8.dp, end = 8.dp),
    ) {
        Surface(shape = CircleShape, color = container, contentColor = content, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp)) }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** La última consulta no llegó: la pantalla sigue mostrando lo último que se sabe y sigue intentando. */
@Composable
private fun ConnectionNotice() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Sin conexión. Seguimos intentando…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CenteredMessage(text: String?, action: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
    ) {
        text?.let { Text(it, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center) }
        action()
    }
}
