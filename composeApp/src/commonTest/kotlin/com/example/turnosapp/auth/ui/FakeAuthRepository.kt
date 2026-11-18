package com.example.turnosapp.auth.ui

import com.example.turnosapp.auth.data.AuthRepository

/** Repositorio de autenticación para las pruebas de ViewModels: responde lo que el escenario pide. */
class FakeAuthRepository(
    var loginResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {

    var loginCalls = 0

    override suspend fun login(username: String, password: String, rememberMe: Boolean): Result<Unit> {
        loginCalls++
        return loginResult
    }
}
