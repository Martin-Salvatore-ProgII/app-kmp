package com.example.turnosapp.core.network

import com.example.turnosapp.core.error.AppError
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/**
 * Ejecuta una llamada a un backend y devuelve su resultado, o el fallo como [AppError].
 * Es el único lugar donde una excepción de red o una respuesta de error se convierte en AppError:
 * los repositorios envuelven cada llamada con esta función y nada de Ktor llega más arriba.
 */
suspend fun <T> runCatchingApi(call: suspend () -> T): Result<T> =
    try {
        Result.success(call())
    } catch (e: CancellationException) {
        // Cancelar una corrutina (por ejemplo, al salir de la pantalla) no es un error: se deja pasar.
        throw e
    } catch (e: ResponseException) {
        Result.failure(toAppError(e.response.status.value, readProblem(e)))
    } catch (e: IOException) {
        // Sin conexión, timeout o certificado no aceptado: no se llegó a tener una respuesta.
        Result.failure(AppError.Network)
    } catch (e: Exception) {
        // Por ejemplo, una respuesta 200 cuyo JSON no coincide con el DTO.
        Result.failure(AppError.Unknown)
    }

/**
 * Traduce una respuesta de error por `status` y `code`, nunca por el texto de `detail`
 * (tabla de errores de `docs/arq/interfaz.md`).
 *
 * @param problem el cuerpo problem+json, o null si no vino o no se pudo leer (un 401 de Spring
 * Security, por ejemplo, puede llegar sin cuerpo).
 */
internal fun toAppError(status: Int, problem: ProblemDto?): AppError {
    val code = problem?.code
    val fieldErrors = problem?.fieldErrors.orEmpty()
    return when {
        status == 401 -> AppError.Unauthorized
        status == 403 -> AppError.Forbidden
        // Si un campo trae más de un error, se muestra el primero.
        status == 400 && fieldErrors.isNotEmpty() ->
            AppError.Validation(fieldErrors.groupBy { it.field }.mapValues { (_, errors) -> errors.first().message })
        status == 503 -> AppError.Unavailable
        status in 400..499 && code != null -> AppError.Business(status, code)
        else -> AppError.Unknown
    }
}

private suspend fun readProblem(e: ResponseException): ProblemDto? =
    try {
        appJson.decodeFromString<ProblemDto>(e.response.bodyAsText())
    } catch (_: SerializationException) {
        null // Cuerpo vacío o que no es problem+json: se decide solo por el status.
    }
