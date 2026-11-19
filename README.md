# App de turnos

App Android del sistema de turnos, hecha con Kotlin Multiplatform y Compose Multiplatform. Es la interfaz del usuario final: registro e inicio de sesión, búsqueda de profesionales, disponibilidad, reserva y consulta y cancelación de las reservas propias.

La app habla solo con los dos backends del proyecto (`turnos-service` y `catalogo-service`), nunca con el servicio de la cátedra. La arquitectura, los contratos y las decisiones están en el repositorio `docs`.

## Requisitos

- **Android Studio** 2026.1 o posterior, con el SDK de Android (plataforma 37).
- **JDK**: no hace falta instalar uno aparte. Gradle compila con Java 21 (ADR-0038) y lo descarga si la máquina no lo tiene.
- **Los dos backends levantados con Docker Compose**, para usar la app contra ellos. Cómo levantarlos está en el README de `turnos-service`.

## Configuración

### 1. El certificado de los backends

Con Docker Compose, los backends hablan solo HTTPS, con un certificado autofirmado que se genera en cada máquina (ADR-0073). La app confía en ese certificado, y solo en ese, para hablar con los backends locales (ADR-0075).

Si todavía no existe, se genera una vez desde `turnos-service`:

```
cd ../turnos-service
./generar-certificado.sh
```

Al compilar, Gradle copia `../turnos-service/secrets/tls.crt` a la app. Si falta o no es un certificado, el build se corta con un mensaje que explica qué hacer. Si se regenera el certificado, alcanza con volver a compilar la app.

### 2. `local.properties`

Android Studio crea `local.properties` en la raíz del proyecto, con la ruta del SDK. El archivo está fuera de Git. Se le pueden agregar estos valores, todos opcionales:

| Propiedad | Para qué | Si no está |
| --- | --- | --- |
| `turnos.url` | URL base del servicio de turnos | `https://10.0.2.2:8080` |
| `catalogo.url` | URL base del servicio de catálogo | `https://10.0.2.2:8081` |
| `tls.cert` | Ruta al certificado de los backends (absoluta o relativa a este proyecto) | `../turnos-service/secrets/tls.crt` |

Desde el emulador de Android, la máquina donde corren los backends es `10.0.2.2`, no `localhost`. Por eso los valores por defecto sirven sin cambiar nada. Los puertos son los del ADR-0056.

El certificado solo vale para `localhost`, `127.0.0.1` y `10.0.2.2`. Por eso la app está pensada para correr en el emulador.

## Ejecución

1. Levantar los backends con Docker Compose (README de `turnos-service`).
2. Abrir esta carpeta en Android Studio y esperar a que termine de sincronizar Gradle.
3. En **Device Manager**, crear un dispositivo virtual (por ejemplo, un Pixel con la imagen de API 37), la primera vez.
4. Elegir la configuración `androidApp` y ejecutarla (▶).

Desde la terminal, el APK de depuración se genera con:

```
./gradlew :androidApp:assembleDebug
```

Queda en `androidApp/build/outputs/apk/debug/`.

## Pruebas

```
./gradlew :composeApp:testAndroidHostTest
```

Corren en la JVM, sin emulador. Son pocas y sobre la lógica que la app aplica por su cuenta (ADR-0047). Por ahora prueban la traducción de las respuestas de error de los backends (`problem+json`) a los errores de la app, por `status` y `code`, con un backend simulado.

`./gradlew build` compila todo y corre las pruebas y el lint. Es lo mismo que ejecuta el CI en cada pull request.

## Estructura

Dos módulos (ADR-0074):

| Módulo | Qué tiene |
| --- | --- |
| `composeApp` | Todo el código común: pantallas, ViewModels, dominio, repositorios y red. Tiene solo `commonMain` y `commonTest` |
| `androidApp` | La app Android: `TurnosApplication`, que arma el contenedor de dependencias; `MainActivity`; el manifiesto; la configuración de red y la copia del certificado |

El código común sigue MVVM (ADR-0051), con un paquete por feature y las capas `ui`, `domain` y `data` dentro de cada uno:

| Paquete | Qué tiene |
| --- | --- |
| `core/di` | `AppContainer`: crea a mano el cliente HTTP y los repositorios (ADR-0052) |
| `core/network` | Cliente HTTP (Ktor), URL de los servicios y traducción de errores (`runCatchingApi`) |
| `core/error` | `AppError` y el mensaje para el usuario de cada error |
| `core/navigation` | Rutas tipadas, barra inferior y mapa de navegación |
| `core/ui` | Tema de Material 3 y componentes reutilizables |
| `auth`, `search`, `myreservations`, `admin`, … | Una feature por pantalla o grupo de pantallas (`docs/arq/interfaz.md`) |

Las versiones de las librerías están en `gradle/libs.versions.toml`.

## Decisiones

| Tema | ADR |
| --- | --- |
| La app habla con los dos backends | 0001 |
| Compose Multiplatform, solo Android, dos módulos | 0048, 0072, 0074 |
| MVVM y stack (Ktor, navegación, dependencias a mano) | 0051, 0052 |
| HTTPS, certificado y configuración de red | 0035, 0073, 0075 |
| Pruebas de la app | 0047 |
