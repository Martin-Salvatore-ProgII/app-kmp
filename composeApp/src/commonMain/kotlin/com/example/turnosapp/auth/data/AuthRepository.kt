package com.example.turnosapp.auth.data

/** Registro e inicio de sesión del usuario final contra turnos (HU-01, HU-02). */
interface AuthRepository {

    /**
     * Inicia sesión y, si sale bien, deja guardado el JWT en la sesión.
     * Un usuario o contraseña incorrectos llegan como `AppError.Unauthorized`.
     */
    suspend fun login(username: String, password: String, rememberMe: Boolean): Result<Unit>
}
