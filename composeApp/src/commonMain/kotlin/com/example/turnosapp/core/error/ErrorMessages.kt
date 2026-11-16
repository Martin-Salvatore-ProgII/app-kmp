package com.example.turnosapp.core.error

/**
 * El mensaje que ve el usuario para cada error. Es el único lugar con estos textos: los ViewModels
 * llaman a `toUserMessage()` y nunca muestran el código ni un texto técnico (`docs/arq/interfaz.md`).
 *
 * Una pantalla puede decidir otra cosa para un caso propio (por ejemplo, el 401 de Login es
 * "usuario o contraseña incorrectos"), pero el resto de los casos sale de acá.
 */
fun AppError.toUserMessage(): String =
    when (this) {
        AppError.Unauthorized -> "Tu sesión venció. Iniciá sesión de nuevo."
        AppError.Forbidden -> "No tenés permiso para hacer esto."
        is AppError.Validation -> "Revisá los datos marcados."
        is AppError.Business -> businessMessages[code] ?: UNKNOWN_MESSAGE
        AppError.Unavailable -> "El servicio no está disponible en este momento. Probá de nuevo en unos minutos."
        AppError.Network -> "No se pudo conectar con el servidor. Revisá tu conexión y probá de nuevo."
        AppError.Unknown -> UNKNOWN_MESSAGE
    }

private const val UNKNOWN_MESSAGE = "Ocurrió un error inesperado. Probá de nuevo."

// Los códigos de `docs/arq/contratos.md` (Códigos de error propios). Los 503 no están porque se
// muestran todos igual, como Unavailable.
private val businessMessages = mapOf(
    "VALIDATION_ERROR" to "Hay datos inválidos. Revisá el formulario.",
    "USERNAME_ALREADY_EXISTS" to "Ese nombre de usuario ya está en uso.",
    "EMAIL_ALREADY_EXISTS" to "Ya hay una cuenta con ese email.",
    "DATE_OUT_OF_RANGE" to "La fecha elegida está fuera del rango permitido.",
    "PROFESSIONAL_DISABLED" to "El profesional no está habilitado.",
    "INVALID_SLOT" to "Ese turno ya no está en la agenda del profesional.",
    "INVALID_PHONE_NUMBER" to "El teléfono no tiene un formato válido.",
    "PROFESSIONAL_NOT_FOUND" to "No se encontró el profesional.",
    "RESERVATION_NOT_FOUND" to "No se encontró la reserva.",
    "ACTIVE_RESERVATION_EXISTS" to "Ya tenés una reserva en curso.",
    "PHONE_NOT_REQUESTED" to "La reserva no está esperando un teléfono.",
    "RESERVATION_NOT_CANCELLABLE" to "Esta reserva ya no se puede cancelar.",
)
