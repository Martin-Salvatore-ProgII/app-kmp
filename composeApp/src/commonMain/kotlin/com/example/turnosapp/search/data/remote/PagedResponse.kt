package com.example.turnosapp.search.data.remote

/** El cuerpo de una lista paginada y el total de su cabecera `X-Total-Count`, tal como llegan. */
class PagedResponse<T>(
    val items: List<T>,
    val totalCount: Int,
)
