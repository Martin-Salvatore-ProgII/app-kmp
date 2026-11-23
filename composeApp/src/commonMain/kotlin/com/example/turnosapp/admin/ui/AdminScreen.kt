package com.example.turnosapp.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.admin.domain.model.AdminReservation
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.AppDatePickerDialog
import com.example.turnosapp.core.ui.ChosenOrEmptyChip
import com.example.turnosapp.core.ui.dayMonthLabel
import com.example.turnosapp.core.ui.label
import com.example.turnosapp.core.ui.monthShortLabel
import com.example.turnosapp.reservation.ui.ReservationStatusBadge

/**
 * Conecta la pantalla con su ViewModel. Cada vez que la pantalla se vuelve a ver, le pide que
 * recargue la lista (ADR-0082).
 */
@Composable
fun AdminRoute(
    container: AppContainer,
    onReservationClick: (reservationId: String) -> Unit,
    viewModel: AdminViewModel = viewModel { AdminViewModel(container.adminReservationRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.onAction(AdminAction.ScreenStarted)
        onStopOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AdminEffect.NavigateToDetail -> onReservationClick(effect.reservationId)
            }
        }
    }
    AdminScreen(state = state, onAction = viewModel::onAction)
}

/** Admin (HU-10, prototipo en `docs/arq/prototipo/`). Solo dibuja el estado y emite acciones. */
@Composable
fun AdminScreen(
    state: AdminUiState,
    onAction: (AdminAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OwnerField(state.ownerInput, onAction)
        FilterChips(state, onAction, modifier = Modifier.padding(top = 10.dp))
        // Al recargar con una lista a la vista, una barra fina arriba en lugar de vaciar la pantalla.
        if (state.isLoading && state.reservations.isNotEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        val error = state.errorMessage
        Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            when {
                error != null -> Message(text = error) {
                    Button(onClick = { onAction(AdminAction.Retry) }, enabled = !state.isLoading) { Text("Reintentar") }
                }
                state.isEmpty -> Message(
                    title = if (state.hasFilters) "No hay reservas con estos filtros" else "Todavía no hay reservas",
                    text = if (state.hasFilters) "Probá quitando alguno de los filtros." else null,
                )
                state.reservations.isEmpty() -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }
                else -> ReservationList(state, onAction)
            }
        }
    }
    when (state.openPicker) {
        AdminPicker.STATUS -> StatusSheet(state.filters.status, onAction)
        AdminPicker.FROM -> AppDatePickerDialog(
            initialDate = state.filters.from,
            isSelectable = state::canPickFrom,
            onDateSelected = { onAction(AdminAction.FromSelected(it)) },
            onDismiss = { onAction(AdminAction.PickerDismissed) },
        )
        AdminPicker.TO -> AppDatePickerDialog(
            initialDate = state.filters.to,
            isSelectable = state::canPickTo,
            onDateSelected = { onAction(AdminAction.ToSelected(it)) },
            onDismiss = { onAction(AdminAction.PickerDismissed) },
        )
        null -> Unit
    }
}

/**
 * El usuario por el que se filtra. Busca al confirmar, no mientras se escribe: turnos compara el login
 * completo, así que buscar letra por letra daría listas vacías (ADR-0082).
 */
@Composable
private fun OwnerField(value: String, onAction: (AdminAction) -> Unit) {
    TextField(
        value = value,
        onValueChange = { onAction(AdminAction.OwnerChanged(it)) },
        placeholder = { Text("Usuario") },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onAction(AdminAction.OwnerCleared) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Borrar el usuario")
                    }
                }
                IconButton(onClick = { onAction(AdminAction.OwnerSubmitted) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Buscar")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onAction(AdminAction.OwnerSubmitted) }),
        shape = CircleShape,
        // Sin la línea de abajo de un TextField común: el campo es una barra de búsqueda redondeada, como en Buscar.
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

/** Un chip por filtro: estado, Desde y Hasta. Con un valor elegido, la cruz lo quita. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChips(state: AdminUiState, onAction: (AdminAction) -> Unit, modifier: Modifier = Modifier) {
    val filters = state.filters
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        ChosenOrEmptyChip(
            chosenLabel = filters.status?.adminLabel(),
            emptyLabel = "Estado",
            onOpen = { onAction(AdminAction.PickerOpened(AdminPicker.STATUS)) },
            onRemove = { onAction(AdminAction.StatusSelected(null)) },
        )
        ChosenOrEmptyChip(
            chosenLabel = filters.from?.let { "Desde ${it.dayMonthLabel()}" },
            emptyLabel = "Desde",
            onOpen = { onAction(AdminAction.PickerOpened(AdminPicker.FROM)) },
            onRemove = { onAction(AdminAction.FromSelected(null)) },
        )
        ChosenOrEmptyChip(
            chosenLabel = filters.to?.let { "Hasta ${it.dayMonthLabel()}" },
            emptyLabel = "Hasta",
            onOpen = { onAction(AdminAction.PickerOpened(AdminPicker.TO)) },
            onRemove = { onAction(AdminAction.ToSelected(null)) },
        )
    }
}

@Composable
private fun ReservationList(state: AdminUiState, onAction: (AdminAction) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
    ) {
        items(state.reservations, key = { it.reservation.id }) {
            AdminReservationCard(it, onClick = { onAction(AdminAction.ReservationClicked(it.reservation.id)) })
        }
        item { ListFooter(state, onAction) }
    }
}

/** Una reserva: el día a la izquierda, y el profesional, el horario, el dueño y el estado exacto al lado. */
@Composable
private fun AdminReservationCard(item: AdminReservation, onClick: () -> Unit) {
    val reservation = item.reservation
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.heightIn(min = 96.dp).padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
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
                Text(
                    "${reservation.startTime.label()} a ${reservation.endTime.label()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${item.owner.fullName} · ${item.owner.login}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ReservationStatusBadge(
                    reservation.status,
                    text = reservation.status.adminLabel(),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Cuántas se ven de cuántas hay y, si quedan, el botón para traer la página siguiente. */
@Composable
private fun ListFooter(state: AdminUiState, onAction: (AdminAction) -> Unit) {
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
                onClick = { onAction(AdminAction.LoadMore) },
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
