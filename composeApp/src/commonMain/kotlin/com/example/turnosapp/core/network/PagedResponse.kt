package com.example.turnosapp.core.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

/** El cuerpo de una lista paginada y el total de su cabecera `X-Total-Count`, tal como llegan. */
class PagedResponse<T>(
    val items: List<T>,
    val totalCount: Int,
)

/** La cabecera con el total de una lista paginada (`docs/arq/contratos.md`, convenciones). */
const val TOTAL_COUNT_HEADER = "X-Total-Count"

/**
 * Lee una página de cualquiera de los dos servicios: los dos paginan igual. Sin la cabecera, se toma
 * lo que ya llegó como todo lo que hay: así nunca se ofrece cargar una página vacía.
 *
 * @param page la página que se pidió, desde 0.
 */
suspend inline fun <reified T> HttpResponse.toPagedResponse(page: Int, size: Int): PagedResponse<T> {
    val items: List<T> = body()
    val total = headers[TOTAL_COUNT_HEADER]?.toIntOrNull() ?: (page * size + items.size)
    return PagedResponse(items, total)
}
