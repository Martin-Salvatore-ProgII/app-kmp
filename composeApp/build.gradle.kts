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
            // Rutas tipadas: cada destino es una clase @Serializable (ADR-0052).
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
