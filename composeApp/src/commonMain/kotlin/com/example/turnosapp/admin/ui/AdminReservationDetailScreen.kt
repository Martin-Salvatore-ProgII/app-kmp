package com.example.turnosapp.admin.ui

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.dayTimeLabel
import com.example.turnosapp.core.ui.label
import com.example.turnosapp.core.ui.longLabel
import com.example.turnosapp.reservation.domain.model.ReservationStatus
import com.example.turnosapp.reservation.ui.ReservationStatusBadge

/** Conecta la pantalla con su ViewModel y traduce sus efectos en navegación. */
@Composable
fun AdminReservationDetailRoute(
    container: AppContainer,
    reservationId: String,
    onBack: () -> Unit,
    viewModel: AdminReservationDetailViewModel = viewModel {
        AdminReservationDetailViewModel(container.adminReservationRepository, reservationId)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AdminReservationDetailEffect.NavigateBack -> onBack()
            }
        }
    }
    AdminReservationDetailScreen(state = state, onAction = viewModel::onAction)
}

/** Detalle de reserva, vista admin (prototipos en `docs/arq/prototipo/`). Solo dibuja el estado y emite acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReservationDetailScreen(
    state: AdminReservationDetailUiState,
    onAction: (AdminReservationDetailAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            // El Scaffold de la navegación ya deja el lugar de la barra de estado: no se repite.
            windowInsets = WindowInsets(0),
            title = { Text("Reserva") },
            navigationIcon = {
                IconButton(onClick = { onAction(AdminReservationDetailAction.BackClicked) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
            actions = {
                IconButton(onClick = { onAction(AdminReservationDetailAction.Refresh) }, enabled = !state.isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                }
            },
        )
        val item = state.reservation
        val error = state.errorMessage
        when {
            item != null -> {
                if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    state.notice?.let { Notice(it) }
                    ReservationStatusBadge(item.reservation.status, text = item.reservation.status.adminLabel())
                    Summary(item)
                    Details(item)
                    SupportData(item)
                }
                if (state.canCancel) CancelButton(onAction)
            }
            error != null -> CenteredMessage(error) {
                Button(onClick = { onAction(AdminReservationDetailAction.Refresh) }, enabled = !state.isLoading) { Text("Reintentar") }
            }
            else -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CircularProgressIndicator() }
        }
    }
    if (state.isCancelDialogOpen && state.reservation != null) CancelDialog(state, state.reservation, onAction)
}

@Composable
private fun Summary(item: AdminReservation) {
    val reservation = item.reservation
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

/** Cuándo se inició o se reservó y, si está cancelada, cuándo y por qué. */
@Composable
private fun Details(item: AdminReservation) {
    val reservation = item.reservation
    // "Reservada" solo si llegó a confirmarse; si no, el proceso solo se inició.
    val reached = reservation.status == ReservationStatus.CONFIRMED || reservation.status == ReservationStatus.CANCELLED
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        DetailRow(Icons.Filled.DateRange, "${if (reached) "Reservada" else "Iniciada"} el ${reservation.createdAt.dayTimeLabel()}")
        reservation.cancelledAt?.let { DetailRow(Icons.Filled.Close, "Cancelada el ${it.dayTimeLabel()}") }
        reservation.cancellationReason?.let {
            Text(
                "Motivo: $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 36.dp),
            )
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

/** Los datos que solo ve el administrador (HU-10, criterio 2; ADR-0082). */
@Composable
private fun SupportData(item: AdminReservation) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            Text("Datos de soporte", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            SupportRow("Usuario", "${item.owner.fullName} · ${item.owner.login}")
            item.failureLabel()?.let { SupportRow("Motivo de la falla", it) }
            item.cancelledByLabel()?.let { SupportRow("Cancelada por", it) }
            SupportRow("Proceso en la cátedra", item.processLabel())
        }
    }
}

@Composable
private fun SupportRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CancelButton(onAction: (AdminReservationDetailAction) -> Unit) {
    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)) {
        OutlinedButton(
            onClick = { onAction(AdminReservationDetailAction.CancelClicked) },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("Cancelar turno") }
    }
}

/**
 * La confirmación, con el motivo obligatorio y su contador (HU-10, criterios 3 y 4): el botón se
 * habilita recién cuando hay un motivo escrito.
 */
@Composable
private fun CancelDialog(
    state: AdminReservationDetailUiState,
    item: AdminReservation,
    onAction: (AdminReservationDetailAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(AdminReservationDetailAction.CancelDismissed) },
        title = { Text("¿Cancelar el turno?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    "Reserva de ${item.owner.fullName} (${item.owner.login}). " +
                        "Queda registrado que la canceló un administrador.",
                )
                OutlinedTextField(
                    value = state.cancelReason,
                    onValueChange = { onAction(AdminReservationDetailAction.CancelReasonChanged(it)) },
                    label = { Text("Motivo (obligatorio)") },
                    supportingText = {
                        Text(
                            "${state.cancelReason.length} / $ADMIN_CANCEL_REASON_MAX_LENGTH",
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    enabled = !state.isCancelling,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(AdminReservationDetailAction.CancelConfirmed) },
                enabled = state.canConfirmCancel,
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
            TextButton(onClick = { onAction(AdminReservationDetailAction.CancelDismissed) }, enabled = !state.isCancelling) {
                Text("Volver")
            }
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
