import java.util.Properties

// La app Android (ADR-0074): punto de entrada, manifiesto y lo que exige la plataforma.
// Desde AGP 9, el soporte de Kotlin ya viene incluido en el plugin de Android.
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

// Configuración de cada máquina (ADR-0075). local.properties está fuera de Git; si falta un valor,
// se usa el del emulador, que llega a la máquina por 10.0.2.2 con los puertos del ADR-0056.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProperty(name: String, default: String): String =
    localProperties.getProperty(name)?.takeIf { it.isNotBlank() } ?: default

android {
    namespace = "com.example.turnosapp"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.turnosapp"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "TURNOS_URL", "\"${localProperty("turnos.url", "https://10.0.2.2:8080")}\"")
        buildConfigField("String", "CATALOG_URL", "\"${localProperty("catalogo.url", "https://10.0.2.2:8081")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ktor.client.okhttp)
}
