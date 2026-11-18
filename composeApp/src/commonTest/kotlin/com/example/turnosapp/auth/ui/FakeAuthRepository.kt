package com.example.turnosapp.auth.ui

import com.example.turnosapp.auth.data.AuthRepository
import com.example.turnosapp.auth.domain.model.Registration

/** Repositorio de autenticación para las pruebas de ViewModels: responde lo que el escenario pide. */
class FakeAuthRepository(
    var loginResult: Result<Unit> = Result.success(Unit),
    var registerResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {

    var loginCalls = 0
    var registerCalls = 0

    override suspend fun register(registration: Registration): Result<Unit> {
        registerCalls++
        return registerResult
    }

    override suspend fun login(username: String, password: String, rememberMe: Boolean): Result<Unit> {
        loginCalls++
        return loginResult
    }
}
