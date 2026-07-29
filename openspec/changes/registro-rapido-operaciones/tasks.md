## 1. Sub-slice EP-001-a: registro completo de campos + Room real (HU-001, HU-002, HU-003)

- [x] 1.1 Crear enums `Direction`, `ResultType`, `CatalogType` en `core.model`
- [x] 1.2 Crear entidades Room `Operation` y `CatalogItem` en `core.data` (spec §3.1, 16 campos de `Operation`)
- [x] 1.3 Crear `OperationDao` y `CatalogItemDao` (insert, getAll ordenado por fecha desc, getById)
- [x] 1.4 Crear `AppDatabase` real (`@Database(entities = [Operation::class, CatalogItem::class], version = 1)`) con `RoomDatabase.Callback.onCreate` que siembra XAUUSD (ASSET), Ninguno (ERROR) y las 8 emociones semilla
- [x] 1.5 Recrear `MentaltraderApplication` instanciando `AppDatabase` y exponiendo los DAOs (inyección manual, sin Hilt)
- [x] 1.6 Crear `OperationFormViewModel` con `StateFlow<OperationFormState>` (data class con los 16 campos + errores + flag `isDirty`)
- [x] 1.7 Construir la UI del formulario: Fecha+Hora (misma fila), Activo, Dirección, Calidad, Emoción antes+motivo, Emoción después+motivo, Error+motivo, Resultado, Descripción entrada (multilínea, expansible, al final)
- [x] 1.8 Cablear "Guardar": validar obligatorios, persistir vía Repository/DAO, navegar de vuelta
- [x] 1.9 Listado mínimo de verificación (placeholder de EP-003): mostrar las operaciones guardadas ordenadas por fecha, solo para poder verificar visualmente el guardado (no es la feature de Historial completa)
- [x] 1.10 Tests unitarios: `OperationFormViewModel` (validación de obligatorios, mapeo de estado) — `OperationFormViewModelTest.kt`, código escrito
- [x] 1.11 Tests instrumentados: guardar operación completa, guardar sin campo obligatorio, semilla XAUUSD, semilla Emociones, semilla Error, motivos en texto libre, descripción vacía/larga — `OperationFormPersistenceTest.kt`/`AppDatabaseSeedTest.kt`, **ejecutados en dispositivo real** (Motorola Edge 50 Fusion, `connectedDebugAndroidTest`, 2026-07-29) tras corregir un bug real que encontró la propia corrida (siembra de catálogos no confiable tras destructive migration — ver `CatalogSeeder`)
- [x] 1.12 `journey_smoke` de este sub-slice: `OperationFormJourneyTest` (FAB -> formulario completo -> Guardar -> Historial muestra la operación) **PASSED en dispositivo real**

## 2. Sub-slice EP-001-b: validaciones de rango/fecha + valores por defecto (HU-004, HU-005)

- [x] 2.1 Agregar campos opcionales `riskPercentage`/`resultInR` a la UI del formulario (ya en el modelo desde 1.2); se agregó además el dropdown "+/-" para el signo de R (nota técnica de HU-004, no estaba cableado en 1.7)
- [x] 2.2 Validación de rango en el ViewModel: Calidad 0.0-10.0, Riesgo 0-100, fecha/hora no futura, R sin límite con sufijo "R"
- [x] 2.3 Prellenado: Fecha/Hora actual al abrir, Activo=XAUUSD si el catálogo está vacío, editable manualmente
- [x] 2.4 Tests unitarios: reglas de validación (límites, fecha futura) — agregados a `OperationFormViewModelTest.kt`
- [ ] 2.5 Tests instrumentados: guardar dentro de rango, fecha futura bloqueada, Calidad/Riesgo fuera de rango, R negativo grande aceptado, prellenado correcto, edición manual conservada — **PENDIENTE**: diferido a la pasada manual del usuario
- [ ] 2.6 `journey_smoke`: abrir formulario ya prellenado, intentar guardar con un valor fuera de rango, corregir y guardar — **PENDIENTE**: diferido a la pasada manual del usuario

## 3. Sub-slice EP-001-c: adjuntar y procesar imágenes (HU-006, HU-007)

- [x] 3.1 Crear entidad Room `OperationImage` (spec §3.1) y su DAO
- [x] 3.2 UI: botón "Adjuntar imagen" (cámara/galería vía `ActivityResultContracts`), límite de 2, miniatura sin procesar
- [x] 3.3 Solicitud de permiso de Cámara solo al adjuntar por esa vía, con degradación graceful si se rechaza (Galería usa el Photo Picker del sistema, sin permiso runtime — ver design.md)
- [x] 3.4 Procesamiento en `Dispatchers.Default`: redimensionar (~1600-2000px ancho) + comprimir JPEG ~85% + guardar en `filesDir` + generar miniatura cacheada una sola vez
- [x] 3.5 Manejo de imagen corrupta/formato no soportado (validación temprana con `BitmapFactory.Options.inJustDecodeBounds` al adjuntar; no se adjunta, se informa)
- [x] 3.6 Carga de miniaturas en UI vía Coil (formulario y listado mínimo)
- [x] 3.7 Tests unitarios: cálculo de redimensionado por relación de aspecto — `ImageProcessorTest.kt`
- [ ] 3.8 Tests instrumentados: adjuntar desde galería, límite de 2, permiso rechazado, miniatura no se recalcula en cada render — **PENDIENTE**: diferido a la pasada manual del usuario
- [ ] 3.9 `journey_smoke`: adjuntar una imagen real, guardar la operación, verla en el listado mínimo con su miniatura — **PENDIENTE**: diferido a la pasada manual del usuario

## 4. Sub-slice EP-001-d: entrada rápida (FAB) y salvaguarda de salida (HU-008, HU-009)

- [x] 4.1 Agregar FAB al `Scaffold` raíz de `MainActivity`, visible solo en Inicio/Historial, navega al formulario
- [x] 4.2 `BackHandler` + intercept de navegación: comparar estado actual del formulario contra su estado inicial (`isDirty`)
- [x] 4.3 Diálogo de confirmación (`AlertDialog`) al detectar salida con cambios pendientes; cancelar/confirmar
- [ ] 4.4 Tests instrumentados: FAB en Inicio/Historial, ausencia en Etiquetas, FAB no tapa contenido en scroll, diálogo al navegar/atrás con cambios, cancelar conserva datos, confirmar descarta, sin cambios sale sin fricción — **PENDIENTE**: diferido a la pasada manual del usuario
- [ ] 4.5 `journey_smoke` final de todo EP-001: FAB → formulario completo (con imagen) → intentar salir sin guardar → cancelar → guardar → ver en listado — **PENDIENTE**: diferido a la pasada manual del usuario

## 5. Cierre del change completo

- [x] 5.1a Suite completa de tests automatizados ejecutada con evidencia real en dispositivo (Motorola Edge 50 Fusion, 2026-07-29): 25/25 unitarios JVM + 16/16 instrumentados (`connectedDebugAndroidTest`) en verde. 3 bugs reales encontrados y corregidos por esta corrida: (1) siembra de catálogos no confiable tras la migración destructiva de la DB -> `CatalogSeeder` idempotente disparado desde `Application.onCreate`; (2) condición de carrera en el prellenado de HU-005 (la corrutina de `init{}` podía pisar campos ya completados) -> guardado solo si el estado sigue en `INITIAL`; (3) `OperationFormJourneyTest` desactualizado tras el prellenado de Activo (HU-005) y tras cambiar el FAB a solo-ícono.
- [x] 5.1b Bugs adicionales encontrados por el usuario en su pasada manual (mismo dispositivo, mismo día) y corregidos: no había forma visible de volver desde Ajustes ni desde Nueva operación (faltaba flecha "atrás" en el TopAppBar) — el gear tampoco usaba `launchSingleTop`; el teclado tapaba campos del formulario sin poder scrollear (falta `imePadding()`, efecto de `enableEdgeToEdge()` + `targetSdk 36` sobre `adjustResize`). **Sin test de regresión dedicado todavía** para estos 2 casos — cubierto solo por verificación manual repetida del usuario tras el fix.
- [ ] 5.1c Pasada manual completa del usuario (en paralelo a 5.1a/b, mismo dispositivo) — cubre específicamente lo que los tests automatizados por semántica de texto no verifican bien: sensación real del prellenado/validación (HU-004/005), flujo de cámara/galería con permisos reales (HU-006/007, sin test instrumentado dedicado — tasks 2.5, 3.8, 4.4 siguen pendientes de esa cobertura), y confirmación visual de que los 3 bugs reportados quedaron resueltos.
- [ ] 5.2 Revisar `wiring_checklist` completo sin ítems `failing` no justificados — pendiente de 5.1c
- [x] 5.3 `openspec validate registro-rapido-operaciones --strict`
