## 1. Estructura de paquetes base

- [x] 1.1 Crear paquetes `core.navigation`, `core.data` (`core.ui` reutiliza el `ui.theme` ya generado por el template; `core.model` se crea recién en EP-001, no vacío especulativo)
- [x] 1.2 Crear paquetes `feature.historial`, `feature.etiquetas`, `feature.metricas` con su pantalla placeholder (`feature.registro` se crea en EP-001, no vacío especulativo)
- [x] 1.3 Agregar Navigation Compose, Room y KSP a `libs.versions.toml`/`build.gradle.kts` (versiones reales resueltas: navigation-compose 2.9.8, room 2.8.4, ksp 2.3.9) — build real verificado en verde

## 2. Navegación (`app-shell-navigation`)

- [x] 2.1 Definir `sealed class Destino(route, label, icon)` en `core.navigation` con los 3 destinos (Inicio, Historial, Etiquetas) — con fix de un bug real de orden de inicialización estática (ver `progress_log`)
- [x] 2.2 Crear pantallas placeholder mínimas: `InicioScreen`, `HistorialScreen`, `EtiquetasScreen`
- [x] 2.3 Implementar `Scaffold` raíz con `NavHost` + `NavigationBar` (Material3), iterando sobre `Destino` para los 3 ítems
- [x] 2.4 Configurar el `startDestination` del `NavHost` en Inicio
- [x] 2.5 Test instrumentado: `AppNavigationTest.laAppAbreEnInicio` — verde en emulador real (Pixel_6_API_30)
- [x] 2.6 Test instrumentado: `AppNavigationTest.tocarHistorialMuestraLaPantallaDeHistorialInmediatamente` (+ Etiquetas) — verde
- [x] 2.7 Test instrumentado: `AppNavigationTest.elDestinoActivoQuedaMarcadoComoSeleccionadoYLosDemasNo` (`assertIsSelected`/`assertIsNotSelected`) — verde
- [x] 2.8 Test unitario: `DestinoTest.hay_exactamente_3_destinos` — verde

## 3. Tema oscuro (`dark-theme`)

- [x] 3.1 Definir paleta oscura fija en `ui/theme/Color.kt` (`DarkBackground`, `DarkSurface`, `DarkOnBackground/Surface`, `AccentPrimary/Secondary`) — sin acentos verde/rojo específicos todavía (esos son de EP-003, coloreado de resultados, no de esta épica de plataforma)
- [x] 3.2 Reescribir `MentaltraderTheme` en `ui/theme/Theme.kt` sin parámetros de tema claro/dinámico — la posibilidad de modo claro se eliminó de la firma de la función, no solo del runtime
- [x] 3.3 Envolver `MentaltraderApp` con `MentaltraderTheme` en `MainActivity`
- [x] 3.4/3.5 Test instrumentado: `ThemeTest.elTemaSiempreExponeLaPaletaOscuraFija` — verde en emulador real
- [x] 3.6 Consistencia de acentos: garantizada por diseño (una única fuente de `ColorScheme` para las 3 pantallas, sin theming por pantalla)

## 4. Orientación fija en vertical (`screen-orientation-lock`)

- [x] 4.1 Configurar `android:screenOrientation="portrait"` en `MainActivity` (`AndroidManifest.xml`)
- [x] 4.2/4.3 Test instrumentado: `OrientationTest.laActividadPrincipalEstaFijadaEnVertical` — verde en emulador real (verifica `ActivityInfo.screenOrientation` vía `PackageManager`, más robusto que rotar físicamente el emulador)
- [x] 4.4 Documentado en `design.md` (Open Questions) y en el bloque `## Trazabilidad`/spec de `screen-orientation-lock` — sin hooks especulativos para HU-019

## 5. Persistencia local offline (`offline-local-persistence`)

- [x] 5.1 Confirmar que `build.gradle.kts` (app y proyecto) no declara ninguna dependencia de cliente HTTP (Retrofit/OkHttp/Ktor-client u otra) — confirmado, solo Compose/Navigation/Room/KSP
- [x] 5.2 Confirmar que `AndroidManifest.xml` no declara el permiso `android.permission.INTERNET` — confirmado
- [x] 5.3 Test instrumentado: verificar en runtime (vía `PackageManager`) que la app instalada no tiene el permiso `INTERNET` (Escenario "Sin dependencia HTTP ni permiso de red en el proyecto")
- [ ] ~~5.4 Crear `AppDatabase` (Room, entidades vacías)~~ — **descartado**: Room/KSP rechaza `@Database(entities = [])` en compilación ("must specify list of entities"). No hay forma de tener una base de datos Room genuinamente vacía.
- [ ] ~~5.5/5.6 Tests de inicialización/persistencia de Room~~ — **diferidos a EP-001**, que sí trae una entidad real (`Operation` o similar) para instanciar `AppDatabase`. Dependencias Room/KSP quedan configuradas y resueltas (build real ya corrido con éxito) para que EP-001 las use sin fricción.
- [x] 5.7 Dejar anotado en `wiring_checklist` que los escenarios "Room se inicializa sin error" y "Persistencia entre reinicios" quedan `failing`/pendientes hasta EP-001 — no se inventó ninguna entidad dummy para cerrarlos artificialmente

## 6. Verificación de cierre del slice

- [ ] 6.1 Ejecutar el journey-smoke completo: abrir la app → ver Inicio → tocar Historial → tocar Etiquetas → confirmar tema oscuro y orientación vertical en las 3
- [ ] 6.2 Revisar que ningún ítem de `wiring_checklist` que SÍ es verificable en este slice quede en `failing` (los que dependen de EP-001/EP-003 futuras quedan documentados como tal, no ocultos)
- [x] 6.3 Correr `openspec validate --strict` sobre el change — válido (corrido dos veces, antes y después del ajuste de alcance de Room)
