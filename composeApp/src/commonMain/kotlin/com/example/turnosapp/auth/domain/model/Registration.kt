package com.example.turnosapp.auth.domain.model

/** Los datos que completa el usuario para crear su cuenta (HU-01). */
data class Registration(
    val login: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val imageUrl: String,
)

/**
 * Los campos del formulario de registro. [contractName] es el nombre del campo en el contrato de
 * turnos: con él se ubica en el formulario cada `fieldErrors` que devuelve el backend.
 */
enum class RegistrationField(val contractName: String) {
    LOGIN("login"),
    PASSWORD("password"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    EMAIL("email"),
    IMAGE_URL("imageUrl"),
    ;

    companion object {
        fun fromContractName(name: String): RegistrationField? = entries.find { it.contractName == name }
    }
}
