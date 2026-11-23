package com.example.turnosapp.myreservations.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.error.reservationFailureMessage
import com.example.turnosapp.core.ui.dayTimeLabel
import com.example.turnosapp.core.ui.label
import com.example.turnosapp.core.ui.longLabel
import com.example.turnosapp.reservation.domain.model.Reservation
import com.example.turnosapp.reservation.domain.model.ReservationStatus

/** Conecta la pantalla con su ViewModel y traduce sus efectos en navegación. */
@Composable
fun ReservationDetailRoute(
    container: AppContainer,
    reservationId: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    viewModel: ReservationDetailViewModel = viewModel { ReservationDetailViewModel(container.reservationRepository, reservationId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReservationDetailEffect.NavigateBack -> onBack()
                ReservationDetailEffect.NavigateToSearch -> onSearch()
            }
        }
    }
    ReservationDetailScreen(state = state, onAction = viewModel::onAction)
}

/** Detalle de reserva (prototipos en `docs/arq/prototipo/`). Solo dibuja el estado y emite acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationDetailScreen(
    state: ReservationDetailUiState,
    onAction: (ReservationDetailAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            // El Scaffold de la navegación ya deja el lugar de la barra de estado: no se repite.
            windowInsets = WindowInsets(0),
            title = { Text("Reserva") },
            navigationIcon = {
                IconButton(onClick = { onAction(ReservationDetailAction.BackClicked) }) {
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
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    state.notice?.let { Notice(it) }
                    ReservationStatusBadge(reservation.status)
                    Summary(reservation)
                    Details(reservation)
                }
                BottomButton(state, reservation, onAction)
            }
            error != null -> CenteredMessage(error) {
                Button(onClick = { onAction(ReservationDetailAction.Retry) }, enabled = !state.isLoading) { Text("Reintentar") }
            }
            else -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CircularProgressIndicator() }
        }
    }
    if (state.isCancelDialogOpen && state.reservation != null) CancelDialog(state, state.reservation, onAction)
}

@Composable
private fun Summary(reservation: Reservation) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(reservation.professionalFullName, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${reservation.date.longLabel()} · ${reservation.startTime.label()} a ${reservation.endTime.label()}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Cuándo se reservó y, según cómo terminó, cuándo se canceló y por qué, o por qué no se completó. */
@Composable
private fun Details(reservation: Reservation) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        DetailRow(Icons.Filled.DateRange, "Reservada el ${reservation.createdAt.dayTimeLabel()}")
        reservation.cancelledAt?.let { DetailRow(Icons.Filled.Close, "Cancelada el ${it.dayTimeLabel()}") }
        reservation.cancellationReason?.let { DetailRow(Icons.Filled.Info, "“$it”") }
        when (reservation.status) {
            ReservationStatus.EXPIRED -> DetailRow(Icons.Filled.Info, "El tiempo para completar la reserva venció.")
            ReservationStatus.FAILED, ReservationStatus.INVALID ->
                DetailRow(Icons.Filled.Info, reservationFailureMessage(reservation.failureReason))
            else -> Unit
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Abajo: "Cancelar turno" si se puede (ADR-0081); con la reserva terminada sin turno, un acceso
 * directo a buscar otro. Una confirmada que ya empezó no tiene botón.
 */
@Composable
private fun BottomButton(state: ReservationDetailUiState, reservation: Reservation, onAction: (ReservationDetailAction) -> Unit) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)) {
        when {
            state.canCancel -> OutlinedButton(
                onClick = { onAction(ReservationDetailAction.CancelClicked) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = modifier,
            ) { Text("Cancelar turno") }
            reservation.status.isFinal && reservation.status != ReservationStatus.CONFIRMED -> Button(
                onClick = { onAction(ReservationDetailAction.SearchClicked) },
                modifier = modifier,
            ) { Text("Buscar otro turno") }
        }
    }
}

/** La confirmación, con el motivo opcional y su contador (HU-09, ADR-0026). */
@Composable
private fun CancelDialog(state: ReservationDetailUiState, reservation: Reservation, onAction: (ReservationDetailAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(ReservationDetailAction.CancelDismissed) },
        title = { Text("¿Cancelar el turno?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "${reservation.date.longLabel()} a las ${reservation.startTime.label()} con " +
                        "${reservation.professionalFullName}. El horario queda libre para otra persona.",
                )
                OutlinedTextField(
                    value = state.cancelReason,
                    onValueChange = { onAction(ReservationDetailAction.CancelReasonChanged(it)) },
                    label = { Text("Motivo (opcional)") },
                    supportingText = {
                        Text("${state.cancelReason.length} / $CANCEL_REASON_MAX_LENGTH", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    },
                    enabled = !state.isCancelling,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(ReservationDetailAction.CancelConfirmed) },
                enabled = !state.isCancelling,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                if (state.isCancelling) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                } else {
                    Text("Cancelar turno")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ReservationDetailAction.CancelDismissed) }, enabled = !state.isCancelling) { Text("Volver") }
        },
    )
}

@Composable
private fun Notice(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CenteredMessage(text: String, action: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        action()
    }
}
