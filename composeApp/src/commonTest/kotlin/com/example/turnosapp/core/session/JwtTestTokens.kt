package com.example.turnosapp.core.session

import kotlin.io.encoding.Base64

/** Arma un JWT de prueba con el `exp` indicado. La firma es de mentira: la app no la verifica. */
fun jwtExpiringAt(exp: Long, roles: List<String> = listOf("ROLE_USER")): String =
    jwtWithPayload("""{"sub":"ana","auth":${roles.joinToString(",", "[", "]") { "\"$it\"" }},"exp":$exp}""")

/** Arma un JWT de prueba con el contenido indicado, tal cual. */
fun jwtWithPayload(json: String): String {
    val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    val header = base64.encode("""{"alg":"RS256"}""".encodeToByteArray())
    val payload = base64.encode(json.encodeToByteArray())
    return "$header.$payload.firma"
}
