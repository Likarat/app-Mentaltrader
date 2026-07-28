## Context

Primer slice de construcción del proyecto. El scaffold Android (Kotlin + Jetpack Compose, minSdk 27) recién creado solo tiene la `MainActivity` default de la plantilla "Empty Activity". No existe todavía ningún paquete, pantalla ni entidad de dominio. El PRD/spec (§2, §6) ya fijó el stack y la estructura de paquetes sugerida (`feature.registro`, `feature.historial`, `feature.etiquetas`, `feature.metricas`, `core.data`, `core.model`, `core.ui`, `core.navigation`), MVVM (ViewModel + Repository, StateFlow), Room, Navigation Compose. Este slice establece esa estructura base y las 4 capabilities de EP-005.

## Goals / Non-Goals

**Goals:**
- Dejar corriendo un shell de navegación de 3 pestañas reales (Inicio/Historial/Etiquetas), cada una con una pantalla placeholder (sin lógica de negocio todavía — eso llega con EP-001 a EP-004).
- Aplicar el tema oscuro de forma global e inmutable (sin alternancia a claro).
- Bloquear la orientación en vertical a nivel de actividad/manifest.
- Configurar Room (base de datos vacía, sin entidades de negocio todavía) y confirmar estáticamente que no hay ninguna dependencia ni permiso de red en el proyecto.
- Establecer la estructura de paquetes base (`core.navigation`, `core.ui`, `core.data`, `core.model`, más los 4 paquetes `feature.*` vacíos como placeholders) para que las épicas de negocio (EP-001 a EP-004) tengan dónde crecer sin reestructurar.

**Non-Goals:**
- No se implementa ninguna lógica de negocio (registro de operaciones, historial real, métricas) — eso es EP-001 a EP-004.
- No se implementa la excepción de rotación horizontal del visor de imagen (HU-019, EP-003 — todavía no existe esa pantalla).
- No se crean entidades Room de negocio (`Operation`, `CatalogItem`, `OperationImage`) — llegan con EP-001. La base de datos de este slice queda vacía (0 entidades), solo configurada.
- No se implementa Hilt/DI framework (el PRD decide inyección manual en v1, ver spec §2).

## Decisions

1. **Navigation Compose con `Scaffold` + `NavHost` + `NavigationBar` de Material3**, alternativa considerada: navegación manual con estado propio (`when` sobre un enum) — descartada porque Navigation Compose ya es la decisión de stack del PRD y maneja back-stack/deep-links de forma estándar.
2. **3 destinos como `sealed class Destino(val route: String, val label: String, val icon: ImageVector)`** en `core.navigation`, iterado para construir la `NavigationBar` — evita repetir el mismo bloque 3 veces y hace explícito que son "exactamente 3" (AC de HU-032 Escenario 4).
3. **Tema oscuro forzado, no `isSystemInDarkTheme()`**: `MaterialTheme(colorScheme = DarkColorScheme, ...)` sin rama de tema claro en el código — alternativa considerada: soportar ambos esquemas y forzar oscuro por configuración, descartada porque el PRD declara modo claro como Non-goal explícito de v1; no tiene sentido escribir código muerto para una rama que no se usa.
4. **Orientación bloqueada en el manifest** (`android:screenOrientation="portrait"` en la única `Activity`), no por código en cada Composable — es la forma estándar y más simple de fijar orientación a nivel de toda la app en Compose de una sola Activity. La futura excepción (HU-019, visor de imagen) requerirá lógica adicional (`requestedOrientation` dinámico o una segunda Activity) que se diseñará en esa épica, no en esta — se deja como **Open Question** abajo.
5. **Room configurado con base de datos vacía (0 entidades) en este slice**, alternativa considerada: crear una entidad dummy solo para probar el round-trip de persistencia — descartada por ser código muerto que se borraría en EP-001; en su lugar, la garantía de "0 llamadas de red" (HU-035 Escenario 1) se verifica **estáticamente** (sin dependencia HTTP en `build.gradle.kts`, sin permiso `INTERNET` en el manifest) y la garantía de persistencia entre sesiones (HU-035 Escenario 2) se verifica con un smoke test de arranque/reinicio de la app vacía (Room se inicializa sin error, sobrevive un reinicio del proceso). La verificación completa de "datos preservados" con entidades reales queda pendiente de EP-001 (ya anotado en `wiring_checklist`).
6. **Estructura de paquetes**: se crean los 4 paquetes `feature.*` como placeholders (una pantalla Composable mínima cada uno, ej. `"Inicio (placeholder)"` como texto), más `core.navigation`, `core.ui` (tema), `core.data` (Room), `core.model` (vacío, se puebla en EP-001).

## Riesgos / Trade-offs

- **[Riesgo]** El Escenario 3 de HU-035 (carga de imágenes desde disco sin red) y el Escenario 2 de HU-034 (retorno a vertical tras el visor de imagen) no son verificables en este slice porque dependen de HU-006/007 y HU-019 respectivamente, que no existen todavía. **→ Mitigación**: quedan como items `failing` documentados en `wiring_checklist`, con nota explícita de qué épica futura los habilita; no se cierran como falsos positivos.
- **[Riesgo]** Bloquear la orientación en el manifest a nivel de `Activity` única simplifica hoy, pero cuando HU-019 necesite la excepción, puede requerir refactor (segunda Activity o control dinámico). **→ Mitigación**: documentado como Open Question; se revisita explícitamente al abrir el slice de HU-019 (EP-003).
- **[Trade-off]** No usar Hilt en v1 (decisión ya tomada en el PRD) significa inyección manual de repositorios/DB — más verboso a medida que crezcan las features, pero evita una dependencia nueva no aprobada en el stack allowlist.

## Open Questions

- ¿Cómo se implementará técnicamente la excepción de rotación del visor de imagen (HU-019, EP-003) sin romper el bloqueo global de esta épica? Se resuelve al abrir ese slice, no ahora.
