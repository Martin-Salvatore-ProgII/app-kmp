package com.example.turnosapp.professional.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.AppDatePickerDialog
import com.example.turnosapp.core.ui.InitialsAvatar
import com.example.turnosapp.core.ui.dayMonthLabel
import com.example.turnosapp.core.ui.label
import com.example.turnosapp.professional.domain.CalculateUpcomingDaysUseCase
import com.example.turnosapp.professional.domain.model.ProfessionalAgenda
import kotlinx.datetime.LocalDate

/** Conecta la pantalla con su ViewModel y traduce sus efectos en navegación. */
@Composable
fun ProfessionalRoute(
    container: AppContainer,
    professionalId: Long,
    searchedDate: LocalDate?,
    onDayChosen: (professionalId: Long, date: LocalDate) -> Unit,
    onBack: () -> Unit,
    viewModel: ProfessionalViewModel = viewModel {
        ProfessionalViewModel(container.professionalRepository, CalculateUpcomingDaysUseCase(), professionalId, searchedDate)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfessionalEffect.NavigateToAvailability -> onDayChosen(effect.professionalId, effect.date)
                ProfessionalEffect.NavigateBack -> onBack()
            }
        }
    }
    ProfessionalScreen(state = state, onAction = viewModel::onAction)
}

/** Profesional (prototipo en `docs/arq/prototipo/`). Solo dibuja el estado y emite acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalScreen(
    state: ProfessionalUiState,
    onAction: (ProfessionalAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = { onAction(ProfessionalAction.BackClicked) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
        )
        val agenda = state.agenda
        val error = state.errorMessage
        when {
            agenda != null -> AgendaContent(agenda, state, onAction)
            error != null -> CenteredMessage(error) {
                Button(onClick = { onAction(ProfessionalAction.Retry) }) { Text("Reintentar") }
            }
            else -> CenteredMessage(null) { CircularProgressIndicator() }
        }
    }

    val agenda = state.agenda
    if (state.isPickingDate && agenda != null) {
        AppDatePickerDialog(
            initialDate = state.upcomingDays.firstOrNull(),
            // Solo días que no pasaron y en los que atiende (ADR-0079). El horizonte lo controla el backend (P-21).
            isSelectable = { it >= state.today && it.dayOfWeek in agenda.attendedDays },
            onDateSelected = { onAction(ProfessionalAction.DayChosen(it)) },
            onDismiss = { onAction(ProfessionalAction.DatePickerDismissed) },
        )
    }
}

@Composable
private fun AgendaContent(agenda: ProfessionalAgenda, state: ProfessionalUiState, onAction: (ProfessionalAction) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InitialsAvatar(agenda.firstName, agenda.lastName, enabled = agenda.enabled, size = 64.dp)
            Column {
                Text(agenda.fullName, style = MaterialTheme.typography.headlineSmall)
                Text(agenda.categoryName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Uno habilitado no lleva marca: solo se avisa lo que impide reservar (ADR-0079).
        if (!agenda.enabled) DisabledNotice()

        Schedules(agenda)

        if (agenda.enabled && state.upcomingDays.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Ver turnos")
                // Dos tarjetas por fila: grandes, cómodas para el dedo.
                state.upcomingDays.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { date ->
                            DayCard(
                                date = date,
                                isSearched = date == state.searchedDate,
                                onClick = { onAction(ProfessionalAction.DayChosen(date)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                OutlinedButton(
                    onClick = { onAction(ProfessionalAction.OtherDateClicked) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Elegir otra fecha", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun Schedules(agenda: ProfessionalAgenda) {
    Column {
        SectionTitle("Atiende")
        if (agenda.schedules.isEmpty()) {
            Text(
                "No tiene horarios habilitados.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        agenda.schedules.forEach { schedule ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(schedule.dayOfWeek.label(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("${schedule.startTime.label()} a ${schedule.endTime.label()}", style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Un acceso directo a los turnos de un día. El día buscado va primero y relleno. */
@Composable
private fun DayCard(date: LocalDate, isSearched: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val content: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.heightIn(min = 88.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(date.dayOfWeek.label(), style = MaterialTheme.typography.bodyMedium)
            Text(date.dayMonthLabel(), style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isSearched) "El día que buscaste" else "Ver turnos", style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (isSearched) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = modifier,
        ) { content() }
    } else {
        OutlinedCard(onClick = onClick, modifier = modifier) { content() }
    }
}

@Composable
private fun DisabledNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                "Este profesional no está habilitado para recibir reservas por ahora. Podés buscar otro de la misma especialidad.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
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
