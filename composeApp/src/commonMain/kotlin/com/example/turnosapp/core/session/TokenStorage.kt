package com.example.turnosapp.core.session

/**
 * Dónde se guarda el JWT de usuario entre una apertura de la app y la siguiente.
 *
 * Es una interfaz porque guardarlo cifrado depende de la plataforma (ADR-0049): la implementación
 * está en el módulo androidApp, que se la pasa al contenedor al arrancar (ADR-0052, ADR-0072).
 * Solo la usa [SessionRepository]: ninguna pantalla ni ViewModel toca el token.
 */
interface TokenStorage {

    /** El JWT guardado, o null si no hay ninguno o no se pudo descifrar. */
    fun read(): String?

    fun save(token: String)

    fun clear()
}
