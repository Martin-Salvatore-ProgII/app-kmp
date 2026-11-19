package com.example.turnosapp.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.PasswordVisibilityButton

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

/**
 * Inicio de sesión (HU-02, prototipo en `docs/arq/prototipo/`). Solo dibuja el estado y emite acciones.
 * Los botones quedan abajo, al alcance del pulgar; el resto se desplaza si aparece el teclado.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 24.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            AppHeader(modifier = Modifier.padding(top = 72.dp, bottom = 16.dp))

            state.infoMessage?.let { InfoNotice(it) }

            OutlinedTextField(
                value = state.username,
                onValueChange = { onAction(LoginAction.UsernameChanged(it)) },
                label = { Text("Usuario") },
                singleLine = true,
                enabled = !state.isLoading,
                isError = state.invalidCredentials,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
                label = { Text("Contraseña") },
                singleLine = true,
                enabled = !state.isLoading,
                isError = state.invalidCredentials,
                textStyle = MaterialTheme.typography.bodyLarge,
                visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    PasswordVisibilityButton(state.passwordVisible) { onAction(LoginAction.PasswordVisibilityToggled) }
                },
                supportingText = state.errorMessage?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            // Toda la fila es el interruptor, no solo el control: más fácil de tocar.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(
                        value = state.rememberMe,
                        enabled = !state.isLoading,
                        role = Role.Switch,
                        onValueChange = { onAction(LoginAction.RememberMeChanged(it)) },
                    ),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recordarme", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "No vas a tener que ingresar por 30 días",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.rememberMe, onCheckedChange = null, enabled = !state.isLoading)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp),
        ) {
            Button(
                onClick = { onAction(LoginAction.Submit) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Ingresar", style = MaterialTheme.typography.titleMedium)
                }
            }
            TextButton(
                onClick = { onAction(LoginAction.RegisterClicked) },
                enabled = !state.isLoading,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("¿No tenés cuenta? Creá una")
            }
        }
    }
}

/** El ícono y el nombre de la app, arriba de Login. */
@Composable
private fun AppHeader(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Icon(
                Icons.Filled.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Text("Turnos", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Buscá profesionales y reservá tu turno",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Un aviso que no es un error, por ejemplo que la sesión venció o que la cuenta se creó. */
@Composable
private fun InfoNotice(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(20.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}
