package com.example.turnosapp.auth.ui

import com.example.turnosapp.core.session.FakeSessionRepository
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `arranca en Buscar solo si hay sesión`() {
        assertTrue(SessionViewModel(FakeSessionRepository("jwt")).startsLoggedIn)
        assertFalse(SessionViewModel(FakeSessionRepository()).startsLoggedIn)
    }

    @Test
    fun `cerrar sesión borra el JWT y emite LoggedOut`() = runTest {
        val session = FakeSessionRepository("jwt")
        val viewModel = SessionViewModel(session)

        viewModel.onAction(SessionAction.Logout)

        assertNull(session.token)
        assertEquals(SessionEffect.LoggedOut, viewModel.effects.first())
    }

    @Test
    fun `si la sesión se cierra por un 401 emite Expired`() = runTest {
        val session = FakeSessionRepository("jwt")
        val viewModel = SessionViewModel(session)

        session.clear() // lo que hace el cliente HTTP ante un 401

        assertEquals(SessionEffect.Expired, viewModel.effects.first())
    }
}
