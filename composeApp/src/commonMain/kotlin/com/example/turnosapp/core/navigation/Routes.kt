package com.example.turnosapp.core.navigation

import kotlinx.serialization.Serializable

// Rutas tipadas (ADR-0052): cada destino es una clase, y sus argumentos son propiedades con tipo.
// El compilador verifica que se navegue con los argumentos correctos, sin armar textos de URL a mano.
// Las pantallas de detalle (profesional, turnos, reserva) suman su ruta en la milestone que las crea.
// Las fechas viajan como texto ISO (`2026-11-26`): la navegación no sabe guardar un LocalDate.

/**
 * @param registeredLogin el usuario que se acaba de registrar, para dejarlo escrito.
 * @param sessionExpired si se llega porque un servicio respondió 401, para avisarlo.
 */
@Serializable
data class Login(
    val registeredLogin: String? = null,
    val sessionExpired: Boolean = false,
)

@Serializable
data object Register

@Serializable
data object Search

@Serializable
data object MyReservations

@Serializable
data object Admin

/**
 * @param date la fecha con la que se buscó, para ofrecerla primero, o null si no se filtró por fecha.
 */
@Serializable
data class Professional(
    val professionalId: Long,
    val date: String? = null,
)

/** Turnos disponibles de un profesional en una fecha. Provisoria hasta la M4. */
@Serializable
data class Availability(
    val professionalId: Long,
    val date: String,
)
