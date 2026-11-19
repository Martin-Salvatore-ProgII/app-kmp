package com.example.turnosapp.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.admin.ui.AdminRoute
import com.example.turnosapp.availability.ui.AvailabilityRoute
import com.example.turnosapp.auth.ui.LoginRoute
import com.example.turnosapp.auth.ui.RegisterRoute
import com.example.turnosapp.auth.ui.SessionAction
import com.example.turnosapp.auth.ui.SessionEffect
import com.example.turnosapp.auth.ui.SessionViewModel
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.myreservations.ui.MyReservationsRoute
import com.example.turnosapp.professional.ui.ProfessionalRoute
import com.example.turnosapp.search.ui.SearchRoute
import kotlinx.datetime.LocalDate

/**
 * El mapa de navegación de la app (`docs/arq/interfaz.md`): qué pantallas hay, cómo se llega
 * de una a otra y cuándo se ve la barra inferior. Las pantallas no conocen el NavController:
 * reciben funciones como `onLoggedIn` y esta función decide a dónde lleva cada una.
 *
 * También sigue la sesión: arranca en Buscar si hay un JWT vigente, y vuelve a Login cuando la
 * sesión se cierra, sea por el botón o por un 401 de cualquier servicio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    container: AppContainer,
    // Desde la M7 sale del rol del JWT. Por ahora la sección de administración no se muestra.
    isAdmin: Boolean = false,
    sessionViewModel: SessionViewModel = viewModel { SessionViewModel(container.sessionRepository) },
) {
    val navController = rememberNavController()
    LaunchedEffect(sessionViewModel) {
        sessionViewModel.effects.collect { effect ->
            // Se vacía toda la pila: con la sesión cerrada, Atrás no puede volver a una pantalla protegida.
            navController.navigate(Login(sessionExpired = effect == SessionEffect.Expired)) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val tabs = TopLevelDestination.entries.filter { !it.adminOnly || isAdmin }
    val showBottomBar = tabs.any { currentDestination.isOn(it) }

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = { Text(tabs.firstOrNull { currentDestination.isOn(it) }?.label.orEmpty()) },
                    actions = {
                        // Un ícono y no un texto: es una acción poco frecuente y la barra queda más limpia (#43).
                        IconButton(onClick = { sessionViewModel.onAction(SessionAction.Logout) }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentDestination.isOn(tab),
                            onClick = { navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (sessionViewModel.startsLoggedIn) Search else Login(),
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Login> { backStackEntry ->
                LoginRoute(
                    container = container,
                    registeredLogin = backStackEntry.toRoute<Login>().registeredLogin,
                    sessionExpired = backStackEntry.toRoute<Login>().sessionExpired,
                    onLoggedIn = {
                        // Login sale de la pila: el botón Atrás desde Buscar cierra la app, no vuelve a Login.
                        navController.navigate(Search) {
                            popUpTo<Login> { inclusive = true }
                        }
                    },
                    onRegisterClick = { navController.navigate(Register) },
                )
            }
            composable<Register> {
                RegisterRoute(
                    container = container,
                    onRegistered = { login ->
                        // Se reemplaza el Login de la pila por uno nuevo con el usuario escrito, así Atrás no
                        // vuelve al formulario de registro.
                        navController.navigate(Login(registeredLogin = login)) {
                            popUpTo<Login> { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Search> {
                SearchRoute(
                    container = container,
                    onProfessionalClick = { id, date -> navController.navigate(Professional(id, date?.toString())) },
                )
            }
            composable<Professional> { backStackEntry ->
                val route = backStackEntry.toRoute<Professional>()
                ProfessionalRoute(
                    container = container,
                    professionalId = route.professionalId,
                    searchedDate = route.date?.let(LocalDate::parse),
                    onDayChosen = { id, date -> navController.navigate(Availability(id, date.toString())) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Availability> { backStackEntry ->
                val route = backStackEntry.toRoute<Availability>()
                AvailabilityRoute(
                    container = container,
                    professionalId = route.professionalId,
                    date = LocalDate.parse(route.date),
                    onBack = { navController.popBackStack() },
                )
            }
            composable<MyReservations> { MyReservationsRoute() }
            composable<Admin> { AdminRoute() }
        }
    }
}

private fun NavDestination?.isOn(tab: TopLevelDestination): Boolean =
    this?.hasRoute(tab.route::class) == true

/**
 * Cambia de sección como lo hace la barra inferior de Material: no apila una sección sobre otra
 * (Atrás desde cualquier sección vuelve a Buscar), y cada sección conserva su estado al volver.
 */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo<Search> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
