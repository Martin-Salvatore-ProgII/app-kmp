package com.example.turnosapp.core.di

/**
 * Crea a mano, una sola vez, los objetos que comparte toda la app (ADR-0052): el cliente HTTP,
 * los repositorios y las implementaciones de plataforma que le pasa MainActivity.
 *
 * Los ViewModels no van acá: cada pantalla crea el suyo con los repositorios de este contenedor,
 * así viven y mueren con su pantalla. Cada repositorio nuevo suma una propiedad a esta clase.
 */
class AppContainer
