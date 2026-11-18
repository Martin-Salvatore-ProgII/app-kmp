package com.example.turnosapp.core.session

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64

/**
 * Lee el vencimiento (`exp`, en segundos desde 1970) del JWT de usuario, o null si no se puede leer.
 *
 * No verifica la firma: la app no tiene cómo ni le corresponde (la autoridad es el backend). Solo
 * sirve para no entrar a Buscar con un token que ya se sabe vencido (ADR-0077). Si el token fuera
 * falso, el backend lo rechaza con 401 en el primer pedido y la app vuelve a Login igual.
 */
fun readJwtExpiration(token: String): Long? =
    try {
        // Un JWT son tres partes en Base64 URL separadas por puntos; los datos están en la del medio.
        val payload = token.split('.').getOrNull(1) ?: return null
        val json = jwtBase64.decode(payload).decodeToString()
        Json.parseToJsonElement(json).jsonObject["exp"]?.jsonPrimitive?.longOrNull
    } catch (e: IllegalArgumentException) {
        null // No es Base64 o no es JSON: un token así no sirve.
    }

// Los JWT van sin el relleno '=' al final; se acepta con o sin él.
private val jwtBase64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
