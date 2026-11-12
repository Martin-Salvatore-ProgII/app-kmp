# CLAUDE.md

Guía para agentes de IA que trabajan en este repositorio. Es corta a propósito: lo que vale para todo el sistema está en el `CLAUDE.md` de la carpeta que contiene a los repos y en el repositorio `docs`. Acá va solo lo propio de la app y dónde leer cada cosa.

## Qué es

La app Android del sistema de turnos, con Kotlin Multiplatform y Compose Multiplatform. Es la interfaz del usuario final: registro e inicio de sesión, búsqueda de profesionales, disponibilidad, reserva con el paso del teléfono, y consulta y cancelación de las reservas propias (ENUNCIADO §3.2, §5).

- **Habla con los dos backends, nunca con la cátedra** (ADR-0001). Con turnos: registro, login, disponibilidad y reservas. Con el catálogo: categorías, búsqueda, agenda y estado de sincronización. El mismo JWT de usuario sirve para los dos: lo emite turnos y el catálogo lo valida (ADR-0003, ADR-0032).
- **Solo Android** (ADR-0072), pero el código va en `commonMain`; `androidMain` tiene solo lo que la plataforma exige.
- **Las pruebas de la app son opcionales** (ENUNCIADO §10.1). Hay pocas, sobre lógica pura (ADR-0047).

## Antes de escribir código

1. Leer `docs/constitucion.md` (en especial P-20) y los ADR que toque la issue.
2. Cargar la skill `/mvvm-kmp` y seguirla. No reconstruir el patrón de memoria.
3. **El alumno no conocía KMP al empezar y defiende el código en una evaluación oral.** Código simple y explícito antes que ingenioso: sin librerías ni mecanismos que no estén en ADR-0052, sin abstracciones de más. Al entregar cada issue, explicar qué hace cada pieza y por qué está en esa capa.
4. Plantearle al alumno las decisiones abiertas de la issue, con una recomendación y su porqué, y esperar su respuesta.

## Dónde leer cada cosa (en el repo `docs`)

| Tema | Documento |
| --- | --- |
| Pantallas, navegación, qué se muestra ante cada error | `arq/interfaz.md` |
| Endpoints, DTO, paginación y códigos de error de los dos servicios | `arq/contratos.md` y `arq/contratos/*.yaml` |
| Estados de una reserva y qué muestra la app en cada uno | `arq/maquina-estados.md` |
| Criterios de aceptación de cada pantalla | `requisitos/HU.md` (HU-01, 02, 04 a 10) |
| Identidades, JWT, roles | `arq/seguridad.md`; ADR-0033 (vigencia y `rememberMe`), ADR-0049 (JWT cifrado) |
| Pruebas de la app | `arq/pruebas.md` (sección App KMP), ADR-0047 |
| Stack y arquitectura | ADR-0048, ADR-0051, ADR-0052, ADR-0072 |
| Seguimiento de la reserva por polling | ADR-0021 |
| Validación del teléfono | ADR-0027 |
| Prototipos de las pantallas | ADR-0050 |
| HTTPS y certificado local | ADR-0035 |
| Qué milestone sigue | `trazabilidad.md` (filas de `app-kmp`) |
| Flujo de Git | `guia-git.md` |

## Comandos

Se confirman al crear el proyecto (issue #1) y se corrigen acá si cambian.

| Para | Comando |
| --- | --- |
| Compilar la app | `./gradlew :composeApp:assembleDebug` |
| Correr las pruebas | `./gradlew :composeApp:testDebugUnitTest` |
| Compilar y probar todo | `./gradlew build` |

Para ejecutarla, Android Studio con un emulador. Desde el emulador, los backends que corren en la máquina están en `10.0.2.2`, no en `localhost`.

## Lo propio de esta app

- **Las dependencias se arman a mano** en un `AppContainer` (ADR-0052). No se agrega una librería de inyección.
- **Lo que depende de Android** (guardar el JWT cifrado, la confianza en el certificado) es una interfaz en `commonMain` con su implementación en `androidMain`. Sin `expect`/`actual` (ADR-0072).
- **Las URL de los dos servicios** salen de la configuración del build, nunca del código de una feature.
- **Los errores se deciden por `status` y `code`**, nunca por el texto de `detail`, y se traducen una sola vez. La tabla de qué hace la app ante cada respuesta está en `arq/interfaz.md`.
- **Un 401 de cualquier servicio** borra el JWT y vuelve a Login, en un solo lugar.
- **La autoridad es siempre el backend.** La app valida solo para mejorar la experiencia (campos obligatorios, formato del teléfono). Mostrar u ocultar la sección de administración según el rol es solo interfaz.
- **El JWT técnico de la cátedra nunca llega a la app.**

## Flujo de trabajo con Git

Igual que en los backends; el detalle está en `docs/guia-git.md`:

- **El agente** crea la rama de la issue solo en local, desde `develop` actualizada (`git checkout -b <tipo>/<N>-<descripcion>`), escribe los archivos y corre `./gradlew build`. No escribe nada en GitHub.
- **El alumno** revisa, commitea, pushea, abre el pull request a `develop` y mergea.
- Al terminar, el agente entrega un bloque de comandos listo para pegar (`git add` por commit, `git commit -m`, `git push`, `gh pr create --base develop`, `gh pr merge --merge --delete-branch`). Mensajes en español, con Conventional Commits, sin atribuciones a herramientas. Cada comando en una sola línea, sin `\`.
- Antes de crear la rama de la issue siguiente, verificar con `git fetch` que el pull request anterior está en `origin/develop`.
- Al cerrar la última issue de una milestone se crea `milestone/<n>-<nombre>` y el pull request a `main` sale de ahí. Nunca de `develop`.

## Convenciones del código

- Identificadores en inglés y `camelCase`; textos visibles, comentarios y documentación en español.
- **Comentarios de decisión:** donde se toma una decisión que alguien podría cuestionar, dos o tres líneas con el porqué. No se comenta lo obvio.
- Cada dependencia entra al build en la issue que la usa por primera vez.
- Las pruebas de cada pieza entran en la misma issue que la pieza.

## Secretos

- Ninguna URL real de la cátedra, credencial ni token va en este repo.
- En la carpeta que contiene a los repos hay dos archivos de la cuenta técnica (`cuenta-tecnica.json`, `cuenta-tecnica-respuesta.json`). **No abrirlos, no mencionarlos con `@` y no copiar su contenido.** La app no los necesita.
