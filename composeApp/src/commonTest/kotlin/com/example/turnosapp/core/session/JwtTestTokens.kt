package com.example.turnosapp.core.session

import kotlin.io.encoding.Base64

/** Arma un JWT de prueba con el `exp` indicado. La firma es de mentira: la app no la verifica. */
fun jwtExpiringAt(exp: Long): String {
    val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    val header = base64.encode("""{"alg":"RS256"}""".encodeToByteArray())
    val payload = base64.encode("""{"sub":"ana","auth":["ROLE_USER"],"exp":$exp}""".encodeToByteArray())
    return "$header.$payload.firma"
}
