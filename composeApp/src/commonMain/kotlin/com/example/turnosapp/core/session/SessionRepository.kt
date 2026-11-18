package com.example.turnosapp.core.session

/**
 * La sesión del usuario: el único lugar de la app que guarda, entrega y borra el JWT.
 * El cliente HTTP lo pide para cada pedido; las pantallas nunca lo ven.
 */
interface SessionRepository {

    /** El JWT de la sesión actual, o null si no hay sesión. */
    fun currentToken(): String?

    /** Guarda el JWT que devolvió el inicio de sesión. */
    fun saveToken(token: String)

    /** Cierra la sesión: borra el JWT. */
    fun clear()
}
