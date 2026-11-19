package com.example.turnosapp.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.auth.domain.ValidateRegistrationUseCase
import com.example.turnosapp.auth.domain.model.Registration
import com.example.turnosapp.auth.domain.model.RegistrationField
import com.example.turnosapp.core.di.AppContainer
import com.example.turnosapp.core.ui.PasswordVisibilityButton

@Composable
fun RegisterRoute(
    container: AppContainer,
    onRegistered: (login: String) -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel {
        RegisterViewModel(container.authRepository, ValidateRegistrationUseCase())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RegisterEffect.Registered -> onRegistered(effect.login)
                RegisterEffect.NavigateBack -> onBack()
            }
        }
    }
    RegisterScreen(state = state, onAction = viewModel::onAction)
}

/**
 * Registro (HU-01, prototipo en `docs/arq/prototipo/`). Los campos van agrupados, cada uno con su
 * ayuda o su error debajo, y el botón queda fijo abajo, al alcance del pulgar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onAction: (RegisterAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            // El Scaffold de la navegación ya deja el lugar de la barra de estado: no se repite.
            windowInsets = WindowInsets(0),
            title = { Text("Crear cuenta") },
            navigationIcon = {
                IconButton(onClick = { onAction(RegisterAction.BackClicked) }, enabled = !state.isLoading) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            SectionTitle("Tus datos")
            listOf(RegistrationField.FIRST_NAME, RegistrationField.LAST_NAME, RegistrationField.EMAIL).forEach {
                RegistrationTextField(it, state, onAction)
            }
            SectionTitle("Tu cuenta")
            listOf(RegistrationField.LOGIN, RegistrationField.PASSWORD).forEach {
                RegistrationTextField(it, state, onAction)
            }
            if (state.showImageUrl) {
                RegistrationTextField(RegistrationField.IMAGE_URL, state, onAction)
            } else {
                TextButton(
                    onClick = { onAction(RegisterAction.ImageUrlExpanded) },
                    enabled = !state.isLoading,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Agregar foto de perfil (opcional)", modifier = Modifier.padding(start = 8.dp))
                }
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(
            onClick = { onAction(RegisterAction.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp).heightIn(min = 56.dp),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Crear cuenta", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Un campo del formulario: debajo muestra su error o, si no tiene, su ayuda. */
@Composable
private fun RegistrationTextField(
    field: RegistrationField,
    state: RegisterUiState,
    onAction: (RegisterAction) -> Unit,
) {
    val isPassword = field == RegistrationField.PASSWORD
    val error = state.fieldErrors[field]
    val supporting = error ?: helpOf(field)
    OutlinedTextField(
        value = state.form.valueOf(field),
        onValueChange = { onAction(RegisterAction.FieldChanged(field, it)) },
        label = { Text(labelOf(field)) },
        singleLine = true,
        enabled = !state.isLoading,
        isError = error != null,
        supportingText = supporting?.let { { Text(it) } },
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (isPassword && !state.passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            { PasswordVisibilityButton(state.passwordVisible) { onAction(RegisterAction.PasswordVisibilityToggled) } }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardOf(field), imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** La ayuda corta de los campos con reglas que no son obvias; las reglas completas las valida el backend. */
private fun helpOf(field: RegistrationField): String? =
    when (field) {
        RegistrationField.LOGIN -> "Letras, números y . _ @ -"
        RegistrationField.PASSWORD -> "Al menos 4 caracteres"
        else -> null
    }

private fun labelOf(field: RegistrationField): String =
    when (field) {
        RegistrationField.LOGIN -> "Usuario"
        RegistrationField.PASSWORD -> "Contraseña"
        RegistrationField.FIRST_NAME -> "Nombre"
        RegistrationField.LAST_NAME -> "Apellido"
        RegistrationField.EMAIL -> "Email"
        RegistrationField.IMAGE_URL -> "URL de tu foto"
    }

private fun keyboardOf(field: RegistrationField): KeyboardType =
    when (field) {
        RegistrationField.LOGIN -> KeyboardType.Ascii
        RegistrationField.PASSWORD -> KeyboardType.Password
        RegistrationField.EMAIL -> KeyboardType.Email
        RegistrationField.IMAGE_URL -> KeyboardType.Uri
        RegistrationField.FIRST_NAME, RegistrationField.LAST_NAME -> KeyboardType.Text
    }

private fun Registration.valueOf(field: RegistrationField): String =
    when (field) {
        RegistrationField.LOGIN -> login
        RegistrationField.PASSWORD -> password
        RegistrationField.FIRST_NAME -> firstName
        RegistrationField.LAST_NAME -> lastName
        RegistrationField.EMAIL -> email
        RegistrationField.IMAGE_URL -> imageUrl
    }
