package com.example.turnosapp.auth.data

import com.example.turnosapp.auth.data.remote.AuthApi
import com.example.turnosapp.auth.data.remote.dto.LoginRequestDto
import com.example.turnosapp.core.network.runCatchingApi
import com.example.turnosapp.core.session.SessionRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionRepository: SessionRepository,
) : AuthRepository {

    // El JWT va directo de la respuesta a la sesión: no pasa por el ViewModel ni por la pantalla.
    override suspend fun login(username: String, password: String, rememberMe: Boolean): Result<Unit> =
        runCatchingApi {
            val response = api.authenticate(LoginRequestDto(username.trim(), password, rememberMe))
            sessionRepository.saveToken(response.idToken)
        }
}
