## Context

Primer change que instancia Room con entidades reales (EP-005 dejó `AppDatabase` deliberadamente sin construir porque Room rechaza `entities=[]`). Es también el primer formulario real de la app y la primera pantalla con lógica de negocio (MVVM: ViewModel + Repository + StateFlow, ya decidido en el PRD/spec §2). Se construye en 4 sub-slices dentro de este mismo change (ver `proposal.md`), cada uno con su propio ciclo TDD y `journey_smoke`.

## Goals / Non-Goals

**Goals:**
- Modelo de datos Room fiel al PRD/spec §3 (`Operation`, `CatalogItem`, `OperationImage`, enums `Direction`/`ResultType`/`CatalogType`).
- Formulario de "Nueva operación" completo y funcional de punta a punta (crear, validar, guardar, ver reflejado en un listado mínimo de verificación).
- Reglas de validación de rango/fecha aplicadas en el ViewModel (no en la UI directamente), reutilizables.
- Adjuntar y comprimir imágenes sin bloquear el hilo principal.
- Punto de entrada (FAB) y salvaguarda de salida (diálogo de confirmación) cableados al `NavHost` ya existente de EP-005.

**Non-Goals (de este change, cubiertos por épicas futuras):**
- Listado real de Historial agrupado por mes (EP-003) — este change solo necesita un listado *mínimo* para verificar visualmente que el guardado funciona (journey-smoke), no la feature completa.
- Gestión de catálogos desde la pestaña Etiquetas (EP-002) — los catálogos se siembran por migración, no se administran todavía.
- Métricas de Inicio (EP-004).
- Visor de imagen a pantalla completa con zoom (HU-019, EP-003).

## Decisions

1. **Room con entidades reales desde el día 1 de este change**: `Operation`, `CatalogItem`, `OperationImage` (spec §3.1), con un `Migration`/`RoomDatabase.Callback` que siembra `XAUUSD` (ASSET), `Ninguno` (ERROR) y el set de 8 emociones (ya decidido en `docs/04-historias/HU-002-completar-campos-catalogo.md`) en la primera creación de la base de datos. Alternativa considerada: sembrar desde código de la app en el primer arranque (`Application.onCreate`) — descartada porque un `RoomDatabase.Callback.onCreate` es más robusto (corre una sola vez, dentro de la transacción de creación del esquema) y no depende del ciclo de vida de la `Application`.
2. **Un solo ViewModel por formulario (`OperationFormViewModel`)** con un único `StateFlow<OperationFormState>` inmutable (data class), en vez de múltiples `StateFlow` por campo — reduce la superficie de recomposición y hace el "dirty check" de HU-009 trivial (comparar el estado actual contra el estado inicial).
3. **Validación en el ViewModel, no en la UI**: cada campo numérico/fecha expone su propio error (`String?`) dentro de `OperationFormState`; el botón "Guardar" solo se habilita si no hay errores y los campos obligatorios están completos. Alternativa considerada: validar solo al presionar "Guardar" — descartada porque el AC de HU-004 pide resaltar el campo específico, que es más natural con validación reactiva por campo.
4. **Compresión de imagen en un `Dispatcher.Default` (CPU-bound) fuera del hilo principal**, usando `BitmapFactory` + `Bitmap.compress(JPEG, 85)` con redimensionado previo (~1600-2000px de ancho) calculado por relación de aspecto — sin librerías de compresión de imagen de terceros (mantiene el stack liviano, ya es parte de la plataforma Android, no requiere aprobación de dependencia nueva).
5. **Carga de imágenes en UI vía Coil** (`io.coil-kt:coil-compose`, ya en `stack-allowlist.json`), para las miniaturas del formulario.
6. **FAB cableado directamente en el `Scaffold` raíz de `MainActivity`** (no dentro de cada pantalla `feature.*`), condicionado a que la ruta actual sea Inicio o Historial (no Etiquetas) — reutiliza el patrón ya existente de `currentBackStackEntryAsState()` de EP-005.
7. **Diálogo de confirmación de salida vía `BackHandler` + intercept de navegación**, comparando el estado actual del formulario contra su valor inicial (ver decisión 2) para decidir si mostrar el diálogo.

## Riesgos / Trade-offs

- **[Riesgo]** El `RoomDatabase.Callback.onCreate` para sembrar catálogos corre en una coroutine separada del hilo principal de Room; hay que usar `SupervisorScope`/`applicationScope` con cuidado de no perder la siembra si la app se cierra durante el primer arranque. **→ Mitigación**: test instrumentado que verifica la siembra completa tras un arranque limpio (borrar datos de la app y volver a abrir).
- **[Riesgo]** Comprimir imágenes en dispositivos de gama baja puede tardar y bloquear percepción de "app lenta" si no se muestra un estado de carga. **→ Mitigación**: mostrar un indicador de progreso simple mientras se procesa (fuera de alcance de los AC exactos de HU-007, pero buena práctica; no bloquea el cierre del sub-slice c).
- **[Trade-off]** No se implementa Hilt (decisión ya tomada en el PRD): el `Repository`/`Dao` se inyectan manualmente desde `MentaltraderApplication` (nueva, se recrea en este change ya que EP-005 la había revertido por no tener Room que instanciar).

## Open Questions

- Ninguna abierta relevante para este change; las dependencias hacia EP-002/EP-003/EP-004 ya están explícitamente fuera de alcance (Non-Goals) y documentadas en las HU correspondientes.
