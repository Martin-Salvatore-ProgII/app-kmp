package com.example.turnosapp.auth.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Cuerpo de `POST /api/authenticate` de turnos (`docs/arq/contratos.md`, Inicio de sesión). */
@Serializable
data class LoginRequestDto(
    // Se llama `username`, no `login`, porque el contrato replica el de JHipster.
    val username: String,
    val password: String,
    val rememberMe: Boolean,
)

@Serializable
data class TokenResponseDto(
    // `id_token` es la única excepción al camelCase del contrato: es el nombre de JHipster.
    @SerialName("id_token") val idToken: String,
)
