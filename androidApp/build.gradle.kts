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

// Certificado autofirmado de los backends (ADR-0073, ADR-0075). No se commitea: se copia al compilar
// desde donde lo deja el script de turnos-service, o desde la ruta de `tls.cert` en local.properties.
val tlsCertificatePath: String =
    rootProject.file(localProperty("tls.cert", "../turnos-service/secrets/tls.crt")).absolutePath

val copyTlsCertificate = tasks.register<CopyTlsCertificate>("copyTlsCertificate") {
    certificate.from(tlsCertificatePath)
    certificatePath.set(tlsCertificatePath)
    outputDirectory.set(layout.buildDirectory.dir("generated/tlsCertificate/res"))
}

// El certificado entra a la app como el recurso `@raw/tls_cert`, que usa network_security_config.xml.
androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(copyTlsCertificate, CopyTlsCertificate::outputDirectory)
    }
}

/**
 * Copia el certificado a los recursos de la app. Si falta o no es un certificado, corta el build con
 * un mensaje que dice qué hacer: compilar sin él daría una app que no puede hablar con ningún backend.
 */
abstract class CopyTlsCertificate : DefaultTask() {

    // Se declara como entrada para que Gradle vuelva a copiarlo cuando se regenera el certificado.
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val certificate: ConfigurableFileCollection

    @get:Input
    abstract val certificatePath: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copy() {
        val source = File(certificatePath.get())
        if (!source.isFile) {
            throw GradleException(
                "No se encontró el certificado de los backends en $source. Generalo con " +
                    "./generar-certificado.sh desde turnos-service, o indicá su ruta con tls.cert en local.properties.",
            )
        }
        if (!source.readText().contains("-----BEGIN CERTIFICATE-----")) {
            throw GradleException("$source no es un certificado PEM. Volvé a generarlo con ./generar-certificado.sh.")
        }
        val rawDirectory = outputDirectory.get().dir("raw").asFile
        rawDirectory.mkdirs()
        source.copyTo(rawDirectory.resolve("tls_cert.crt"), overwrite = true)
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ktor.client.okhttp)
}
