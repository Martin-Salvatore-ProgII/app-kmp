package com.example.turnosapp.core.navigation

import kotlinx.serialization.Serializable

// Rutas tipadas (ADR-0052): cada destino es una clase, y sus argumentos son propiedades con tipo.
// El compilador verifica que se navegue con los argumentos correctos, sin armar textos de URL a mano.
// Las pantallas de detalle (profesional, turnos, reserva) suman su ruta en la milestone que las crea.

/** @param registeredLogin el usuario que se acaba de registrar, para dejarlo escrito; null si se llega de otro lado. */
@Serializable
data class Login(val registeredLogin: String? = null)

@Serializable
data object Register

@Serializable
data object Search

@Serializable
data object MyReservations

@Serializable
data object Admin
