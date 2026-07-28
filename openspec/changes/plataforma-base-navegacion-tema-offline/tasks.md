## 1. Estructura de paquetes base

- [ ] 1.1 Crear paquetes `core.navigation`, `core.ui`, `core.data`, `core.model` bajo `app/src/main/java/.../`
- [ ] 1.2 Crear paquetes placeholder `feature.registro`, `feature.historial`, `feature.etiquetas`, `feature.metricas` (vacíos, listos para las épicas de negocio futuras)
- [ ] 1.3 Confirmar que `build.gradle.kts` (app) tiene las dependencias de Navigation Compose y Room ya declaradas (agregar si faltan, respetando `stack-allowlist.json`)

## 2. Navegación (`app-shell-navigation`)

- [ ] 2.1 Definir `sealed class Destino(route, label, icon)` en `core.navigation` con los 3 destinos (Inicio, Historial, Etiquetas)
- [ ] 2.2 Crear pantallas placeholder mínimas: `InicioScreen`, `HistorialScreen`, `EtiquetasScreen` (un `Text` simple cada una, en sus paquetes `feature.*`)
- [ ] 2.3 Implementar `Scaffold` raíz con `NavHost` + `NavigationBar` (Material3), iterando sobre `Destino` para los 3 ítems
- [ ] 2.4 Configurar el `startDestination` del `NavHost` en Inicio
- [ ] 2.5 Test instrumentado/UI: verificar que la app abre en Inicio (Escenario "Apertura de la app en Inicio")
- [ ] 2.6 Test instrumentado/UI: verificar cambio inmediato entre pestañas (Escenario "Cambio inmediato entre pestañas")
- [ ] 2.7 Test instrumentado/UI: verificar resaltado visual del destino activo (Escenario "Resaltado visual del destino activo")
- [ ] 2.8 Test: verificar que la `NavigationBar` renderiza exactamente 3 ítems (Escenario "Exactamente 3 destinos")

## 3. Tema oscuro (`dark-theme`)

- [ ] 3.1 Definir `DarkColorScheme` en `core.ui/theme/Color.kt` (paleta fija: fondo oscuro, texto claro, acentos verde/rojo para ganada/perdida)
- [ ] 3.2 Implementar `MentalTraderTheme` en `core.ui/theme/Theme.kt` usando siempre `DarkColorScheme`, sin rama de tema claro ni `isSystemInDarkTheme()`
- [ ] 3.3 Envolver el `Scaffold` raíz con `MentalTraderTheme`
- [ ] 3.4 Test: capturar/verificar que las 3 pantallas renderizan con la paleta oscura (Escenario "La app abre en modo oscuro")
- [ ] 3.5 Test: simular dispositivo en modo claro del SO y confirmar que el tema de la app no cambia (Escenario "El tema no varía según la configuración del sistema operativo")
- [ ] 3.6 Revisión visual: confirmar consistencia de acentos de color entre las 3 pantallas (Escenario "Consistencia de acentos de color entre pantallas")

## 4. Orientación fija en vertical (`screen-orientation-lock`)

- [ ] 4.1 Configurar `android:screenOrientation="portrait"` en la única `Activity` del `AndroidManifest.xml`
- [ ] 4.2 Test/verificación manual: rotar el dispositivo/emulador en cada una de las 3 pantallas y confirmar que no rota (Escenario "Orientación vertical en el uso general")
- [ ] 4.3 Test/verificación manual: confirmar que un intento de forzar horizontal (rotación del SO) es rechazado (Escenario "Rechazo de forzado horizontal")
- [ ] 4.4 Documentar en código (comentario breve en el manifest o en `core.navigation`) que la excepción de HU-019 (visor de imagen, EP-003) se diseñará en esa épica — no crear ningún hook o override especulativo ahora

## 5. Persistencia local offline (`offline-local-persistence`)

- [ ] 5.1 Confirmar que `build.gradle.kts` (app y proyecto) no declara ninguna dependencia de cliente HTTP (Retrofit/OkHttp/Ktor-client u otra)
- [ ] 5.2 Confirmar que `AndroidManifest.xml` no declara el permiso `android.permission.INTERNET`
- [ ] 5.3 Crear `AppDatabase` (Room, `@Database` con lista de entidades vacía por ahora) en `core.data`
- [ ] 5.4 Instanciar `AppDatabase` en el punto de entrada de la app (ej. `Application` class o composición manual, según decisión de DI manual del PRD)
- [ ] 5.5 Test: arrancar la app y confirmar que `AppDatabase` se inicializa sin excepciones (Escenario "Room se inicializa sin error al arrancar la app")
- [ ] 5.6 Test instrumentado: cerrar completamente el proceso y reabrir, confirmar que la instancia de Room sigue siendo válida/accesible sin reinicialización ni error (Escenario "Persistencia entre reinicios completos del proceso")
- [ ] 5.7 Dejar anotado en el `wiring_checklist` del slice que el Escenario "Verificación completa diferida a datos de negocio reales" queda `failing`/pendiente hasta que EP-001 aporte entidades reales — no inventar una entidad dummy para cerrarlo artificialmente

## 6. Verificación de cierre del slice

- [ ] 6.1 Ejecutar el journey-smoke completo: abrir la app → ver Inicio → tocar Historial → tocar Etiquetas → confirmar tema oscuro y orientación vertical en las 3
- [ ] 6.2 Revisar que ningún ítem de `wiring_checklist` que SÍ es verificable en este slice quede en `failing` (los que dependen de EP-001/EP-003 futuras quedan documentados como tal, no ocultos)
- [ ] 6.3 Correr `openspec validate` sobre el change antes de pasar a `dod`
