package com.example.turnosapp.myreservations.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.label
import com.example.turnosapp.core.ui.monthShortLabel
import com.example.turnosapp.reservation.domain.model.Reservation

/**
 * Conecta la pantalla con su ViewModel. Cada vez que la pantalla se vuelve a ver, le pide que
 * recargue la lista (ADR-0081).
 */
@Composable
fun MyReservationsRoute(
    container: AppContainer,
    onReservationClick: (reservationId: String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: MyReservationsViewModel = viewModel { MyReservationsViewModel(container.reservationRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.onAction(MyReservationsAction.ScreenStarted)
        onStopOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyReservationsEffect.NavigateToReservation -> onReservationClick(effect.reservationId)
                MyReservationsEffect.NavigateToSearch -> onSearchClick()
            }
        }
    }
    MyReservationsScreen(state = state, onAction = viewModel::onAction)
}

/** Mis reservas (HU-08, prototipo en `docs/arq/prototipo/`). Solo dibuja el estado y emite acciones. */
@Composable
fun MyReservationsScreen(
    state: MyReservationsUiState,
    onAction: (MyReservationsAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        FilterChips(selected = state.filter, onAction = onAction)
        // Al recargar con una lista a la vista, una barra fina arriba en lugar de vaciar la pantalla.
        if (state.isLoading && state.reservations.isNotEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        val error = state.errorMessage
        Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            when {
                error != null -> Message(text = error) {
                    Button(onClick = { onAction(MyReservationsAction.Retry) }, enabled = !state.isLoading) { Text("Reintentar") }
                }
                state.isEmpty -> EmptyList(state.filter, onAction)
                state.reservations.isEmpty() -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
                else -> ReservationList(state, onAction)
            }
        }
    }
}

@Composable
private fun FilterChips(selected: ReservationFilter, onAction: (MyReservationsAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)) {
        ReservationFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onAction(MyReservationsAction.FilterSelected(filter)) },
                label = { Text(filter.label) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
                modifier = Modifier.heightIn(min = 40.dp),
            )
        }
    }
}

@Composable
private fun ReservationList(state: MyReservationsUiState, onAction: (MyReservationsAction) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
    ) {
        items(state.reservations, key = { it.id }) {
            ReservationCard(it, onClick = { onAction(MyReservationsAction.ReservationClicked(it)) })
        }
        item { ListFooter(state, onAction) }
    }
}

/** Una reserva: el día a la izquierda, y el profesional, el horario y el estado al lado. */
@Composable
private fun ReservationCard(reservation: Reservation, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.heightIn(min = 84.dp).padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                Text(reservation.date.day.toString().padStart(2, '0'), style = MaterialTheme.typography.titleLarge)
                Text(
                    reservation.date.monthShortLabel().uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(reservation.professionalFullName, style = MaterialTheme.typography.titleMedium)
                // Sin la categoría: la reserva no la trae, y pedirla al catálogo haría que la lista dependa de él (ADR-0081).
                Text(
                    "${reservation.startTime.label()} a ${reservation.endTime.label()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ReservationStatusBadge(reservation.status, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Cuántas se ven de cuántas hay y, si quedan, el botón para traer la página siguiente. */
@Composable
private fun ListFooter(state: MyReservationsUiState, onAction: (MyReservationsAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Text(
            "Mostrando ${state.reservations.size} de ${state.totalCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.loadMoreErrorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        if (state.canLoadMore || state.loadMoreErrorMessage != null) {
            OutlinedButton(
                onClick = { onAction(MyReservationsAction.LoadMore) },
                enabled = !state.isLoadingMore,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                if (state.isLoadingMore) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (state.loadMoreErrorMessage != null) "Reintentar" else "Cargar más")
                }
            }
        }
    }
}

/** Sin reservas: con Todas, un acceso directo a Buscar; con un filtro, solo el aviso. */
@Composable
private fun EmptyList(filter: ReservationFilter, onAction: (MyReservationsAction) -> Unit) {
    val title = when (filter) {
        ReservationFilter.ALL -> "Todavía no tenés reservas"
        ReservationFilter.CONFIRMED -> "No tenés reservas confirmadas"
        ReservationFilter.CANCELLED -> "No tenés reservas canceladas"
    }
    Message(title = title, text = if (filter == ReservationFilter.ALL) "Cuando reserves un turno, lo vas a ver acá." else null) {
        if (filter == ReservationFilter.ALL) {
            Button(onClick = { onAction(MyReservationsAction.SearchClicked) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Buscar profesionales")
            }
        }
    }
}

@Composable
private fun Message(title: String? = null, text: String? = null, action: @Composable () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        if (title != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(32.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
        text?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action()
    }
}
