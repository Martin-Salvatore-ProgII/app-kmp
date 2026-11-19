package com.example.turnosapp.search.ui

import com.example.turnosapp.search.domain.model.Category
import com.example.turnosapp.search.domain.model.DayPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed interface SearchAction {
    data class NameChanged(val value: String) : SearchAction
    data object NameCleared : SearchAction

    data class SheetOpened(val sheet: SearchSheet) : SearchAction
    data object SheetDismissed : SearchAction

    /** null quita el filtro. */
    data class CategorySelected(val category: Category?) : SearchAction

    /** null quita la fecha, y con ella la franja. */
    data class DateSelected(val date: LocalDate?) : SearchAction

    /** Una franja fija; null es "Cualquier hora" y quita la franja. */
    data class DayPeriodSelected(val period: DayPeriod?) : SearchAction

    data object CustomRangeExpanded : SearchAction
    data class CustomTimeEditing(val field: TimeField?) : SearchAction
    data class CustomTimePicked(val time: LocalTime) : SearchAction
    data object CustomRangeApplied : SearchAction

    data class EnabledSelected(val enabled: Boolean) : SearchAction

    data object Retry : SearchAction
    data object LoadMore : SearchAction
}
