package com.example.turnosapp.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turnosapp.auth.data.AuthRepository
import com.example.turnosapp.auth.domain.ValidateRegistrationUseCase
import com.example.turnosapp.auth.domain.model.Registration
import com.example.turnosapp.auth.domain.model.RegistrationField
import com.example.turnosapp.core.error.AppError
import com.example.turnosapp.core.error.toUserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Registro del usuario final (HU-01). */
class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val validateRegistration: ValidateRegistrationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private val _effects = Channel<RegisterEffect>(Channel.BUFFERED)
    val effects: Flow<RegisterEffect> = _effects.receiveAsFlow()

    fun onAction(action: RegisterAction) {
        when (action) {
            // Al corregir un campo se quita su error; los demás quedan marcados hasta el próximo envío.
            is RegisterAction.FieldChanged -> _state.update {
                it.copy(form = it.form.with(action.field, action.value), fieldErrors = it.fieldErrors - action.field)
            }
            RegisterAction.Submit -> register()
            RegisterAction.BackClicked -> viewModelScope.launch { _effects.send(RegisterEffect.NavigateBack) }
        }
    }

    private fun register() {
        val current = _state.value
        if (current.isLoading) return
        val localErrors = validateRegistration(current.form)
        if (localErrors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = localErrors, errorMessage = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, fieldErrors = emptyMap(), errorMessage = null) }
            authRepository.register(current.form)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(RegisterEffect.Registered(current.form.login.trim()))
                }
                .onFailure { error -> showFailure(error) }
        }
    }

    private fun showFailure(error: Throwable) {
        when {
            error is AppError.Validation -> _state.update {
                it.copy(isLoading = false, fieldErrors = backendFieldErrors(error), errorMessage = error.toUserMessage())
            }
            // Login o email repetidos se marcan en su campo, con el mensaje de ErrorMessages.
            error is AppError.Business && error.code == "USERNAME_ALREADY_EXISTS" -> markField(RegistrationField.LOGIN, error)
            error is AppError.Business && error.code == "EMAIL_ALREADY_EXISTS" -> markField(RegistrationField.EMAIL, error)
            else -> _state.update { it.copy(isLoading = false, errorMessage = error.toUserMessage()) }
        }
    }

    private fun markField(field: RegistrationField, error: AppError) {
        _state.update { it.copy(isLoading = false, fieldErrors = mapOf(field to error.toUserMessage())) }
    }

    // Los mensajes del backend están en inglés: se usa el campo que marca, pero con la regla en español.
    private fun backendFieldErrors(error: AppError.Validation): Map<RegistrationField, String> =
        error.fieldErrors.keys
            .mapNotNull { RegistrationField.fromContractName(it) }
            .associateWith { ValidateRegistrationUseCase.ruleOf(it) }
}

private fun Registration.with(field: RegistrationField, value: String): Registration =
    when (field) {
        RegistrationField.LOGIN -> copy(login = value)
        RegistrationField.PASSWORD -> copy(password = value)
        RegistrationField.FIRST_NAME -> copy(firstName = value)
        RegistrationField.LAST_NAME -> copy(lastName = value)
        RegistrationField.EMAIL -> copy(email = value)
        RegistrationField.IMAGE_URL -> copy(imageUrl = value)
    }
