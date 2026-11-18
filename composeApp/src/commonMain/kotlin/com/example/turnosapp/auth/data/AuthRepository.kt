package com.example.turnosapp.auth.data

import com.example.turnosapp.auth.domain.model.Registration

/** Registro e inicio de sesión del usuario final contra turnos (HU-01, HU-02). */
interface AuthRepository {

    /**
     * Crea la cuenta. No inicia sesión: el usuario entra después con su contraseña.
     * Los rechazos llegan como `AppError.Validation` (con `fieldErrors`) o `AppError.Business`
     * (`USERNAME_ALREADY_EXISTS`, `EMAIL_ALREADY_EXISTS`).
     */
    suspend fun register(registration: Registration): Result<Unit>

    /**
     * Inicia sesión y, si sale bien, deja guardado el JWT en la sesión.
     * Un usuario o contraseña incorrectos llegan como `AppError.Unauthorized`.
     */
    suspend fun login(username: String, password: String, rememberMe: Boolean): Result<Unit>
}
