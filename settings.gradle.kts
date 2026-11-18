rootProject.name = "app-kmp"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
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
