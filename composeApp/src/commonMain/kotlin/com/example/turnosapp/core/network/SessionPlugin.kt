package com.example.turnosapp.core.network

import com.example.turnosapp.core.session.SessionRepository
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

/**
 * Une el cliente HTTP con la sesión, en un solo lugar para los dos servicios:
 *
 * - Agrega `Authorization: Bearer <jwt>` a cada pedido si hay sesión. El mismo JWT sirve para
 *   turnos y para el catálogo (ADR-0003, ADR-0032).
 * - Si un pedido que llevaba el JWT recibe 401, el token ya no sirve (venció o es inválido): se
 *   borra la sesión, y la navegación, que observa la sesión, vuelve a Login (`docs/arq/interfaz.md`).
 *
 * Un 401 de un pedido sin JWT (el inicio de sesión con credenciales incorrectas) no toca la sesión:
 * ese caso lo resuelve la pantalla de Login.
 */
internal fun sessionPlugin(sessionRepository: SessionRepository) =
    createClientPlugin("Session") {
        onRequest { request, _ ->
            sessionRepository.currentToken()?.let { request.bearerAuth(it) }
        }
        onResponse { response ->
            val sentToken = response.call.request.headers[HttpHeaders.Authorization] != null
            if (response.status == HttpStatusCode.Unauthorized && sentToken) {
                sessionRepository.clear()
            }
        }
    }
