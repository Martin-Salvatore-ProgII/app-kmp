package com.example.turnosapp.core.session

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64

// Lo que la app lee del JWT de usuario sin verificar la firma: no tiene cómo ni le corresponde (la
// autoridad es el backend). Si el token fuera falso, el backend lo rechaza con 401 en el primer
// pedido y la app vuelve a Login igual.

/**
 * El vencimiento (`exp`, en segundos desde 1970) del JWT de usuario, o null si no se puede leer.
 * Sirve para no entrar a Buscar con un token que ya se sabe vencido (ADR-0077).
 */
fun readJwtExpiration(token: String): Long? =
    readJwtPayload(token)?.get("exp")?.jsonPrimitive?.longOrNull

/**
 * Los roles del JWT de usuario (el claim `auth`, por ejemplo `["ROLE_USER"]`), o una lista vacía si
 * no se pueden leer. Solo deciden qué secciones se muestran (ADR-0082): cada operación la autoriza turnos.
 */
fun readJwtRoles(token: String): List<String> {
    val auth = readJwtPayload(token)?.get("auth") as? JsonArray ?: return emptyList()
    return auth.mapNotNull { (it as? JsonPrimitive)?.content }
}

/** El rol de administrador (ADR-0037). */
const val ROLE_ADMIN = "ROLE_ADMIN"

// Un JWT son tres partes en Base64 URL separadas por puntos; los datos están en la del medio.
private fun readJwtPayload(token: String): JsonObject? =
    try {
        val payload = token.split('.').getOrNull(1) ?: return null
        val json = jwtBase64.decode(payload).decodeToString()
        Json.parseToJsonElement(json).jsonObject
    } catch (e: IllegalArgumentException) {
        null // No es Base64 o no es JSON: un token así no sirve.
    }

// Los JWT van sin el relleno '=' al final; se acepta con o sin él.
private val jwtBase64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
