package com.example.turnosapp.auth.ui

import com.example.turnosapp.auth.domain.ValidateRegistrationUseCase
import com.example.turnosapp.auth.domain.model.RegistrationField
import com.example.turnosapp.core.error.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val repository = FakeAuthRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModelWithValidForm() = RegisterViewModel(repository, ValidateRegistrationUseCase()).apply {
        onAction(RegisterAction.FieldChanged(RegistrationField.LOGIN, " ana "))
        onAction(RegisterAction.FieldChanged(RegistrationField.PASSWORD, "secreta"))
        onAction(RegisterAction.FieldChanged(RegistrationField.FIRST_NAME, "Ana"))
        onAction(RegisterAction.FieldChanged(RegistrationField.LAST_NAME, "Pérez"))
        onAction(RegisterAction.FieldChanged(RegistrationField.EMAIL, "ana@example.com"))
    }

    @Test
    fun `un registro correcto vuelve a Login con el usuario`() = runTest {
        val viewModel = viewModelWithValidForm()

        viewModel.onAction(RegisterAction.Submit)

        assertEquals(RegisterEffect.Registered("ana"), viewModel.effects.first())
    }

    @Test
    fun `con errores locales no llama al backend`() = runTest {
        val viewModel = RegisterViewModel(repository, ValidateRegistrationUseCase())

        viewModel.onAction(RegisterAction.Submit)

        assertEquals(0, repository.registerCalls)
        assertEquals("Este dato es obligatorio.", viewModel.state.value.fieldErrors[RegistrationField.EMAIL])
    }

    @Test
    fun `los fieldErrors del backend marcan su campo con la regla en español`() = runTest {
        repository.registerResult = Result.failure(AppError.Validation(mapOf("login" to "Login has invalid characters")))
        val viewModel = viewModelWithValidForm()

        viewModel.onAction(RegisterAction.Submit)

        val state = viewModel.state.value
        assertEquals(mapOf(RegistrationField.LOGIN to ValidateRegistrationUseCase.ruleOf(RegistrationField.LOGIN)), state.fieldErrors)
        assertEquals("Revisá los datos marcados.", state.errorMessage)
    }

    @Test
    fun `un login o un email repetidos se marcan en su campo`() = runTest {
        repository.registerResult = Result.failure(AppError.Business(400, "USERNAME_ALREADY_EXISTS"))
        val viewModel = viewModelWithValidForm()
        viewModel.onAction(RegisterAction.Submit)
        assertEquals(
            mapOf(RegistrationField.LOGIN to "Ese nombre de usuario ya está en uso."),
            viewModel.state.value.fieldErrors,
        )

        repository.registerResult = Result.failure(AppError.Business(400, "EMAIL_ALREADY_EXISTS"))
        viewModel.onAction(RegisterAction.Submit)
        assertEquals(
            mapOf(RegistrationField.EMAIL to "Ya hay una cuenta con ese email."),
            viewModel.state.value.fieldErrors,
        )
    }

    @Test
    fun `corregir un campo le quita su error`() = runTest {
        val viewModel = RegisterViewModel(repository, ValidateRegistrationUseCase())
        viewModel.onAction(RegisterAction.Submit)

        viewModel.onAction(RegisterAction.FieldChanged(RegistrationField.EMAIL, "a"))

        assertEquals(null, viewModel.state.value.fieldErrors[RegistrationField.EMAIL])
        assertEquals("Este dato es obligatorio.", viewModel.state.value.fieldErrors[RegistrationField.LOGIN])
    }
}
