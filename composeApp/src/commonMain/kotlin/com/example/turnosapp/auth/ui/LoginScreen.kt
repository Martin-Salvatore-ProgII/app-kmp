package com.example.turnosapp.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.core.di.AppContainer

/** Conecta la pantalla con su ViewModel y traduce sus efectos en navegación. */
@Composable
fun LoginRoute(
    container: AppContainer,
    registeredLogin: String?,
    sessionExpired: Boolean,
    onLoggedIn: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = viewModel {
        LoginViewModel(container.authRepository, registeredLogin, sessionExpired)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.LoggedIn -> onLoggedIn()
                LoginEffect.NavigateToRegister -> onRegisterClick()
            }
        }
    }
    LoginScreen(state = state, onAction = viewModel::onAction)
}

/** Inicio de sesión (HU-02). Solo dibuja el estado y emite acciones. */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Turnos", style = MaterialTheme.typography.headlineLarge)
        Text("Iniciá sesión para buscar profesionales y reservar", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = state.username,
            onValueChange = { onAction(LoginAction.UsernameChanged(it)) },
            label = { Text("Usuario") },
            singleLine = true,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !state.isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = state.rememberMe,
                onCheckedChange = { onAction(LoginAction.RememberMeChanged(it)) },
                enabled = !state.isLoading,
            )
            Text("Recordarme durante 30 días")
        }

        state.infoMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = { onAction(LoginAction.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Ingresar")
            }
        }
        TextButton(
            onClick = { onAction(LoginAction.RegisterClicked) },
            enabled = !state.isLoading,
        ) {
            Text("¿No tenés cuenta? Creá una")
        }
    }
}
