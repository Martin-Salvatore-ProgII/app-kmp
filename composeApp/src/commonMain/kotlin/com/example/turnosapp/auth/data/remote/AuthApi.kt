package com.example.turnosapp.auth.data.remote

import com.example.turnosapp.auth.data.remote.dto.LoginRequestDto
import com.example.turnosapp.auth.data.remote.dto.TokenResponseDto
import com.example.turnosapp.core.network.ServiceUrls
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Llamadas de registro e inicio de sesión. Son de turnos, que es quien emite el JWT (ADR-0003). */
class AuthApi(
    private val client: HttpClient,
    private val urls: ServiceUrls,
) {
    suspend fun authenticate(request: LoginRequestDto): TokenResponseDto =
        client.post("${urls.turnos}/api/authenticate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
