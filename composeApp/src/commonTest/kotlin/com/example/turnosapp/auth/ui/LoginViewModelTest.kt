package com.example.turnosapp.auth.ui

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
import kotlin.test.assertFalse
import kotlin.test.assertNull

// viewModelScope corre en Dispatchers.Main, que en una prueba sin Android no existe: se reemplaza
// por uno de prueba que ejecuta todo en el momento.
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val repository = FakeAuthRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModelWithCredentials() = LoginViewModel(repository).apply {
        onAction(LoginAction.UsernameChanged("ana"))
        onAction(LoginAction.PasswordChanged("secreta"))
    }

    @Test
    fun `un login correcto emite LoggedIn`() = runTest {
        val viewModel = viewModelWithCredentials()

        viewModel.onAction(LoginAction.Submit)

        assertEquals(LoginEffect.LoggedIn, viewModel.effects.first())
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `un 401 se muestra como credenciales incorrectas, sin decir cuál falló`() = runTest {
        repository.loginResult = Result.failure(AppError.Unauthorized)
        val viewModel = viewModelWithCredentials()

        viewModel.onAction(LoginAction.Submit)

        assertEquals("Usuario o contraseña incorrectos.", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `otro error muestra el mensaje común de ese error`() = runTest {
        repository.loginResult = Result.failure(AppError.Network)
        val viewModel = viewModelWithCredentials()

        viewModel.onAction(LoginAction.Submit)

        assertEquals(
            "No se pudo conectar con el servidor. Revisá tu conexión y probá de nuevo.",
            viewModel.state.value.errorMessage,
        )
    }

    @Test
    fun `con campos vacíos no llama al backend`() = runTest {
        val viewModel = LoginViewModel(repository)

        viewModel.onAction(LoginAction.Submit)

        assertEquals(0, repository.loginCalls)
        assertEquals("Ingresá tu usuario y tu contraseña.", viewModel.state.value.errorMessage)
    }

    @Test
    fun `al llegar desde el registro el usuario ya está escrito y se avisa que la cuenta se creó`() {
        val state = LoginViewModel(repository, registeredLogin = "ana").state.value

        assertEquals("ana", state.username)
        assertEquals("Tu cuenta quedó creada. Ingresá con tu contraseña.", state.infoMessage)
    }
}
