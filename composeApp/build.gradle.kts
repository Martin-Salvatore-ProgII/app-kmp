// Biblioteca KMP con todo el código común de la app (ADR-0074). No tiene código de Android:
// lo que la plataforma exige va en el módulo androidApp.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)

    // Android es el único destino (ADR-0072).
    android {
        namespace = "com.example.turnosapp.composeapp"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()

        // Habilita las pruebas de commonTest en la JVM, sin emulador (ADR-0047).
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material.icons.core)
            // ViewModel con viewModelScope, y lectura del estado atada al ciclo de vida de la pantalla (ADR-0052).
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            // Rutas tipadas: cada destino es una clase @Serializable (ADR-0052).
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            // Fechas y horas sin zona para los filtros y la agenda (ADR-0052).
            implementation(libs.kotlinx.datetime)
            // Cliente HTTP (ADR-0052). El motor (OkHttp) lo pone androidApp (ADR-0075).
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        // Pruebas sobre lógica pura, sin emulador (ADR-0047). MockEngine responde como un backend, sin red.
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
