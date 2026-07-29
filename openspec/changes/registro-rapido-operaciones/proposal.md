## Why

EP-005 dejó el esqueleto de la app caminando (navegación, tema, orientación) pero sin ningún dato real: las 3 pantallas son placeholders vacíos. El propósito central del producto — registrar operaciones de forex rápido y estudiar el historial para detectar patrones — todavía no existe. Este change construye el corazón del producto: el formulario de "Nueva operación" completo, sus reglas de validación, el prellenado rápido, el adjuntado de imágenes, y los dos puntos de entrada/salida (botón flotante y confirmación al salir). Es, además, la primera vez que se instancia Room con entidades reales, cerrando el diferimiento explícito que dejó EP-005.

## What Changes

- Agregar el modelo de datos Room: entidades `Operation` y `CatalogItem` (con semillas `XAUUSD`/Ninguno/set de Emociones), y su persistencia real (cierra el gap dejado por EP-005 en `offline-local-persistence`).
- Agregar el formulario de "Nueva operación" con sus 16 campos (5 identificadores + 4 de catálogo + 2 opcionales de resultado + 3 motivos en texto libre + descripción + imagen), en una sola columna, orden fijo.
- Agregar las reglas de validación de rango/fecha (Calidad, Riesgo %, Resultado en R, fecha/hora no futura).
- Agregar el prellenado de valores por defecto (fecha/hora actual, Activo=XAUUSD si el catálogo está vacío).
- Agregar el adjuntado de imágenes (cámara/galería, máx. 2) con redimensionado, compresión JPEG y miniatura cacheada.
- Agregar el botón flotante "+" en Inicio/Historial que abre el formulario.
- Agregar la confirmación al salir del formulario con cambios sin guardar.

## Capabilities

### New Capabilities
- `operation-entry-form`: formulario completo de "Nueva operación" — los 16 campos, layout de una columna, prellenado de valores por defecto, punto de entrada vía botón flotante, y confirmación de salida con cambios pendientes. Cubre HU-001, HU-002, HU-003, HU-005, HU-008, HU-009.
- `operation-field-validation`: reglas de validación de rango y fecha/hora sobre los campos numéricos y de fecha del formulario. Cubre HU-004.
- `operation-image-attachment`: adjuntar hasta 2 imágenes desde cámara o galería, con permisos, redimensionado, compresión JPEG y miniatura cacheada. Cubre HU-006, HU-007.

### Modified Capabilities
- `offline-local-persistence`: la Requirement "Persistencia local sobrevive a reinicios completos" (declarada pero no implementada en EP-005 por la restricción de Room con `entities=[]`) pasa a implementarse aquí, con las entidades reales `Operation` y `CatalogItem`.

## Impact

- **Código nuevo**: `core.model` (enums `Direction`, `ResultType`, `CatalogType`), `core.data` (entidades Room `Operation`/`CatalogItem`/`OperationImage`, DAOs, `AppDatabase` real, migración de semillas), `feature.registro` (formulario completo, ViewModel, validación), ampliación de `core.navigation`/`MainActivity` (FAB), ampliación de `core.data` para imágenes (`filesDir`, Coil).
- **Dependencias nuevas**: `io.coil-kt:coil-compose` (carga de imágenes en UI, ya pre-aprobada en `stack-allowlist.json` durante EP-005 pero no usada todavía).
- **Historias de usuario cubiertas**: ver bloque `## Trazabilidad` abajo.
- **Construcción incremental**: este change se construye en 4 sub-slices verificables dentro del mismo change/rama/PR (ver `docs/03-backlog` y `.claude/state/build-state.json#active_slice.sub_slices`): (a) registro completo de campos + Room real, (b) validaciones + valores por defecto, (c) adjuntar/procesar imágenes, (d) FAB + confirmación de salida. Cada sub-slice cierra con `journey_smoke` verde antes de pasar al siguiente.

## Trazabilidad

- **Épica**: EP-001 — Registro rápido de operaciones (`docs/03-backlog/epicas.md`)
- **Historias de usuario cubiertas** (`docs/04-historias/`):
  - HU-001 — Registrar operación con campos identificadores mínimos
  - HU-002 — Completar operación con campos de catálogo obligatorios
  - HU-003 — Agregar descripción de entrada a la operación
  - HU-004 — Validar rangos numéricos y de fecha/hora al guardar
  - HU-005 — Prellenar fecha/hora y activo por defecto al abrir "Nueva operación"
  - HU-006 — Adjuntar imagen sin procesar (cámara o galería, máx. 2)
  - HU-007 — Comprimir, redimensionar y generar miniatura cacheada al guardar
  - HU-008 — Crear operación desde el botón flotante "+"
  - HU-009 — Confirmar antes de salir del formulario con cambios sin guardar
