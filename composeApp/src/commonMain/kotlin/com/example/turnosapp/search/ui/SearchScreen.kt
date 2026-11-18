package com.example.turnosapp.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.AppDatePickerDialog
import com.example.turnosapp.core.ui.shortLabel
import com.example.turnosapp.search.domain.model.ProfessionalSummary

/** Conecta la pantalla con su ViewModel. */
@Composable
fun SearchRoute(
    container: AppContainer,
    viewModel: SearchViewModel = viewModel { SearchViewModel(container.searchRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(state = state, onAction = viewModel::onAction)
}

/**
 * Buscar (HU-05, prototipo en `docs/arq/prototipo/`). Arriba el nombre y los chips de los filtros;
 * cada chip abre su hoja inferior, y lo elegido queda como chip marcado con una cruz para quitarlo.
 */
@Composable
fun SearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
) {
    if (state.catalogNotReady) {
        CatalogNotReady(isLoading = state.isLoading, onRetry = { onAction(SearchAction.Retry) })
        return
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        NameField(name = state.filters.name, onAction = onAction)
        FilterChips(state = state, onAction = onAction, modifier = Modifier.padding(top = 16.dp))
        if (state.catalogSyncing) SyncingNotice(modifier = Modifier.padding(top = 16.dp))
        Results(state = state, onAction = onAction, modifier = Modifier.weight(1f).padding(top = 12.dp))
    }

    when (state.openSheet) {
        SearchSheet.CATEGORY -> CategorySheet(state, onAction)
        SearchSheet.TIME_RANGE -> TimeRangeSheet(state, onAction)
        SearchSheet.ENABLED -> EnabledSheet(state, onAction)
        SearchSheet.DATE -> AppDatePickerDialog(
            initialDate = state.filters.date,
            isSelectable = { it >= state.today },
            onDateSelected = { onAction(SearchAction.DateSelected(it)) },
            onDismiss = { onAction(SearchAction.SheetDismissed) },
        )
        null -> Unit
    }
    CustomTimePicker(state, onAction)
}

@Composable
private fun NameField(name: String, onAction: (SearchAction) -> Unit) {
    TextField(
        value = name,
        onValueChange = { onAction(SearchAction.NameChanged(it)) },
        placeholder = { Text("Buscá por nombre o apellido") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (name.isNotEmpty()) {
                IconButton(onClick = { onAction(SearchAction.NameCleared) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar el nombre")
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        // Sin la línea de abajo de un TextField común: el campo es una barra de búsqueda redondeada.
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

/** Un chip por filtro. El de la franja aparece recién cuando hay una fecha (ADR-0079). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChips(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = state.filters
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        ChosenOrEmptyChip(
            chosenLabel = filters.category?.name,
            emptyLabel = "Categoría",
            onOpen = { onAction(SearchAction.SheetOpened(SearchSheet.CATEGORY)) },
            onRemove = { onAction(SearchAction.CategorySelected(null)) },
        )
        ChosenOrEmptyChip(
            chosenLabel = filters.date?.shortLabel(),
            emptyLabel = "Fecha",
            onOpen = { onAction(SearchAction.SheetOpened(SearchSheet.DATE)) },
            onRemove = { onAction(SearchAction.DateSelected(null)) },
        )
        if (filters.date != null) {
            ChosenOrEmptyChip(
                chosenLabel = filters.timeRange?.let(::timeRangeLabel),
                emptyLabel = "Franja horaria",
                onOpen = { onAction(SearchAction.SheetOpened(SearchSheet.TIME_RANGE)) },
                onRemove = { onAction(SearchAction.DayPeriodSelected(null)) },
            )
        }
        // El estado siempre tiene un valor (por defecto, habilitados): se marca solo si se cambió.
        FilterChip(
            selected = !filters.enabled,
            onClick = { onAction(SearchAction.SheetOpened(SearchSheet.ENABLED)) },
            label = { Text(if (filters.enabled) "Habilitados" else "No habilitados") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.heightIn(min = 40.dp),
        )
    }
}

/**
 * Sin valor, un chip que abre su hoja. Con valor, un chip marcado que muestra lo elegido: tocarlo
 * abre la hoja para cambiarlo, y la cruz quita el filtro.
 */
@Composable
private fun ChosenOrEmptyChip(
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

@Composable
private fun Results(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // La lista anterior queda a la vista mientras llega la nueva: solo se agrega la barra de carga.
        if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        val error = state.errorMessage
        when {
            error != null -> Message(text = error, actionLabel = "Reintentar", onClick = { onAction(SearchAction.Retry) })
            !state.isLoading && state.professionals.isEmpty() ->
                Message(text = "No encontramos profesionales con esos filtros.")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            ) {
                item { ResultCount(state.totalCount) }
                items(state.professionals, key = { it.id }) { ProfessionalCard(it) }
                if (state.canLoadMore || state.loadMoreErrorMessage != null) {
                    item { LoadMoreFooter(state, onAction) }
                }
            }
        }
    }
}

@Composable
private fun ResultCount(totalCount: Int) {
    Text(
        if (totalCount == 1) "1 profesional" else "$totalCount profesionales",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Al final de la lista: cuántos se ven de cuántos hay, y el botón para traer la página siguiente. */
@Composable
private fun LoadMoreFooter(state: SearchUiState, onAction: (SearchAction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    ) {
        Text(
            "Mostrando ${state.professionals.size} de ${state.totalCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.loadMoreErrorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        OutlinedButton(
            onClick = { onAction(SearchAction.LoadMore) },
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

@Composable
private fun ProfessionalCard(professional: ProfessionalSummary) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.heightIn(min = 76.dp).padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Initials(professional, enabled = professional.enabled)
            Column(modifier = Modifier.weight(1f)) {
                Text(professional.fullName, style = MaterialTheme.typography.titleMedium)
                Text(
                    professional.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!professional.enabled) {
                    Text(
                        "No disponible para reservar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
internal fun Initials(professional: ProfessionalSummary, enabled: Boolean, size: Int = 44) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.secondaryContainer else colors.surfaceVariant),
    ) {
        Text(
            "${professional.firstName.take(1)}${professional.lastName.take(1)}",
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) colors.onSecondaryContainer else colors.onSurfaceVariant,
        )
    }
}

/** Aviso corto y no bloqueante: la búsqueda sigue funcionando con la copia anterior (ADR-0010, ADR-0079). */
@Composable
private fun SyncingNotice(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .heightIn(min = 40.dp)
            .padding(horizontal = 16.dp),
    ) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "Actualizando el catálogo…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/** El catálogo todavía no tiene su primera copia: no hay filtros que mostrar, solo esperar y reintentar. */
@Composable
private fun CatalogNotReady(isLoading: Boolean, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            "El catálogo todavía se está cargando",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            "Es la primera vez que se trae el catálogo de profesionales. En unos segundos vas a poder buscar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry, enabled = !isLoading) { Text("Reintentar") }
    }
}

/** Un mensaje centrado en el lugar de la lista, con un botón opcional. */
@Composable
internal fun Message(text: String, actionLabel: String? = null, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (actionLabel != null) Button(onClick = onClick) { Text(actionLabel) }
    }
}
