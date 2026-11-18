package com.example.turnosapp.core.session

/** Sesión respaldada por el almacenamiento cifrado del dispositivo (ADR-0049). */
class SessionRepositoryImpl(
    private val tokenStorage: TokenStorage,
) : SessionRepository {

    override fun currentToken(): String? = tokenStorage.read()

    override fun saveToken(token: String) = tokenStorage.save(token)

    override fun clear() = tokenStorage.clear()
}
