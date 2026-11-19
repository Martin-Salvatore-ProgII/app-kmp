rootProject.name = "app-kmp"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Si la máquina no tiene el JDK 21 que fija el toolchain (ADR-0038), Gradle lo descarga.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Dos módulos (ADR-0074): el código común y la app Android que lo usa.
include(":composeApp")
include(":androidApp")
