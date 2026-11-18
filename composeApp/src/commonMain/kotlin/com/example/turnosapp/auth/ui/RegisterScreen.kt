package com.example.turnosapp.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.turnosapp.auth.domain.ValidateRegistrationUseCase
import com.example.turnosapp.auth.domain.model.Registration
import com.example.turnosapp.auth.domain.model.RegistrationField
import com.example.turnosapp.core.di.AppContainer

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

/** Registro (HU-01). Cada campo muestra debajo su error, si lo tiene. */
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onAction: (RegisterAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)

        RegistrationField.entries.forEach { field ->
            RegistrationTextField(
                field = field,
                value = state.form.valueOf(field),
                error = state.fieldErrors[field],
                enabled = !state.isLoading,
                onValueChange = { onAction(RegisterAction.FieldChanged(field, it)) },
            )
        }

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = { onAction(RegisterAction.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Crear cuenta")
            }
        }
        TextButton(onClick = { onAction(RegisterAction.BackClicked) }, enabled = !state.isLoading) {
            Text("Ya tengo cuenta")
        }
    }
}

@Composable
private fun RegistrationTextField(
    field: RegistrationField,
    value: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    val isPassword = field == RegistrationField.PASSWORD
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(labelOf(field)) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardOf(field), imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun labelOf(field: RegistrationField): String =
    when (field) {
        RegistrationField.LOGIN -> "Usuario"
        RegistrationField.PASSWORD -> "Contraseña"
        RegistrationField.FIRST_NAME -> "Nombre"
        RegistrationField.LAST_NAME -> "Apellido"
        RegistrationField.EMAIL -> "Email"
        RegistrationField.IMAGE_URL -> "URL de tu foto (opcional)"
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
