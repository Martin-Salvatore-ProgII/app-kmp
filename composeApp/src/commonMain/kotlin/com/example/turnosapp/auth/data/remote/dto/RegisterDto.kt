package com.example.turnosapp.auth.data.remote.dto

import kotlinx.serialization.Serializable

/** Cuerpo de `POST /api/register` de turnos (`docs/arq/contratos.md`, Registro). Responde 201 sin cuerpo. */
@Serializable
data class RegisterRequestDto(
    val login: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val imageUrl: String?,
    val langKey: String,
)
