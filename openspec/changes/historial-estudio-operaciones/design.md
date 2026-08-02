## Context

`HistorialScreen` hoy es un listado mínimo de verificación (`feature/historial/HistorialScreen.kt`): una `LazyColumn` plana sobre `operationDao.getAllOrderedByDateDesc()` completo, sin agrupar, sin paginar, sin filtros ni edición/eliminación. Esta épica lo reemplaza por la feature real: listado agrupado por mes/día con Paging 3, resumen mensual, detalle/edición/eliminación con deshacer, visor de imagen a pantalla completa, y filtros + búsqueda persistidos.

Se reutiliza sin modificar: `OperationFormScreen`/`OperationFormViewModel` (EP-001, para editar), `CatalogItemDao`/`CatalogRepository` (EP-002, para poblar selectores de filtro), `ImageProcessor.thumbnailPathFor` (EP-001, miniaturas). `Operation`/`OperationImage`/`OperationDao`/`OperationImageDao` (EP-001) se extienden con nuevas queries, sin romper su contrato actual (ningún método existente cambia de firma).

## Goals / Non-Goals

**Goals:**
- Listado agrupado por mes → día, con scroll fluido vía Paging 3 sobre Room (sin cargar 3,000-5,000 operaciones a memoria).
- Reutilizar el formulario de registro de EP-001 para editar, sin duplicar validaciones.
- Eliminar con ventana de deshacer real (soft-delete temporal en memoria, no en schema).
- Filtros combinados (etiqueta + fecha/periodo) y búsqueda de texto con lógica AND, persistidos en DataStore con las claves ya fijadas por la spec.
- Visor de imagen a pantalla completa como única excepción a la orientación vertical fija de EP-005.

**Non-Goals:**
- No se toca el contrato de `OperationDao`/`OperationImageDao` existente (solo se agregan métodos nuevos).
- No se introduce Hilt/DI framework (mismo patrón de wiring manual de `MentaltraderApplication`/`MainActivity` ya usado en EP-001/EP-002).
- No se implementa búsqueda FTS (SQLite Full-Text Search) — HU-023 explícitamente deja `LIKE` como opción válida y más simple; se documenta como posible mejora futura, no como deuda oculta.
- No se resuelve aquí el recálculo de HU-034-AC2 (EP-005, retorno a vertical tras salir del visor) — ese ítem de `wiring_checklist` ya vive en el histórico de EP-005 y se cierra retroactivamente cuando HU-019 exista (ver progress_log del DoR).

## Decisions

### 1. Agrupación mes/día vía `PagingData.insertSeparators` (HU-015/016)
`OperationDao` gana una query paginada (`PagingSource<Int, Operation>`, ordenada por `dateTime DESC`, con los filtros de HU-022/023 como parámetros opcionales — ver decisión 5). El `Pager` se construye en `HistorialViewModel`; los separadores de mes/día se insertan client-side con `PagingData.insertSeparators { before, after -> ... }` (API estándar de Paging 3 para este caso exacto), produciendo un `Flow<PagingData<HistorialListItem>>` donde `HistorialListItem` es una `sealed interface` con variantes `MonthHeader`, `DayHeader`, `OperationRow`. Alternativa descartada: agrupar en la query SQL con `GROUP BY` — no es compatible con paginación incremental de filas individuales (necesitamos operaciones sueltas, no agregados, para poder paginar).

### 2. Resumen mensual (HU-017) como query aparte, no derivada de la paginación
El resumen por mes (n° operaciones, % ganadas, R acumulado) es una query `GROUP BY strftime('%Y-%m', ...)` independiente (`Flow<List<MonthSummary>>`, sin paginar — el número de meses reales es acotado, no de la escala de las operaciones). El colapsar/expandir de HU-017 es estado de UI puro (`Set<YearMonth>` de meses colapsados en el ViewModel), no afecta la query.

### 3. Editar reutiliza `OperationFormViewModel` en modo edición (HU-020)
`OperationFormViewModel` gana un parámetro opcional `editingOperationId: Long? = null`. Si no es null, el `init` carga la `Operation` existente vía `operationDao.getById(id)` y prellena `OperationFormState` con sus valores (en vez de los defaults de HU-005); `save()` hace `operationDao.update(...)` en vez de `insert(...)` cuando está en modo edición. `OperationDao` gana `update(operation: Operation)` (Room `@Update`). No se duplica la validación: es la misma `validate()`/`save()` ya existente, solo cambia el destino final (`insert` vs `update`) y el prellenado inicial.

### 4. Eliminar con deshacer: soft-delete SOLO en memoria del ViewModel, no en schema (HU-021)
Se descarta agregar una columna `deletedAt`/`isDeleted` a `Operation` (evitar tocar el schema/migración de Room para un estado transitorio de UI). En su lugar: `HistorialViewModel` mantiene `pendingDeleteIds: StateFlow<Set<Long>>`; al eliminar, el id se agrega a ese set (la UI lo filtra del `PagingData` vía `.filter { it.id !in pendingDeleteIds.value }`) y se lanza un `Job` con `delay(UNDO_WINDOW_MS)` que, si no fue cancelado por "Deshacer", ejecuta el borrado físico real (`operationDao.delete(operation)` + borra los archivos de `OperationImage` asociados, reutilizando el mismo patrón de limpieza de archivos que `DataResetService`). "Deshacer" cancela el `Job` y quita el id de `pendingDeleteIds` — la operación nunca se tocó en Room, así que "restaurar exactamente como estaba" es trivial (no hay reinserción, nunca se borró). Trade-off aceptado: si el proceso muere durante la ventana de deshacer, el borrado diferido nunca corre y la operación simplemente sigue existiendo (falla hacia el lado seguro — no hay pérdida de datos, objetivo #4 del PRD).

### 5. Filtros + búsqueda: query con parámetros opcionales nulos, no `RawQuery` (HU-022/023)
Una única `@Query` parametrizada con el patrón `(:param IS NULL OR columna = :param)` por cada filtro (Activo, Resultado, Error, Emoción antes/después, rango de fecha, texto libre vía `LIKE` sobre los 4 campos de texto). Alternativa descartada: `@RawQuery`/query builder dinámico — pierde la verificación en tiempo de compilación de Room sin necesidad real (el número de combinaciones de filtro es fijo y pequeño). El filtro de catálogo (Activo/Emoción/Error) se aplica por `CatalogItem.id` (ya seleccionado en el picker, reutilizando `CatalogItemDao.getByType` de EP-002 para poblar las opciones).

### 6. Persistencia de filtros: DataStore Preferences con claves individuales (HU-024)
Se usan las claves ya fijadas por la spec (`lastPeriodFilter`, `lastCustomRangeStart/End`, `lastSearchText`, `lastSelectedAssets`, `lastSelectedResults`, `lastSelectedErrors`, `lastSelectedEmotionBefore/After`) como `Preferences.Key` individuales (listas de IDs serializadas como CSV de `Long`/`String`, sin agregar una dependencia de serialización JSON — consistente con "sin librerías pesadas" del stack). Un `HistorialFilterRepository` (nuevo, `core/data`) encapsula lectura/escritura, análogo a como `CatalogRepository` encapsula el acceso a catálogos.

### 7. Visor de imagen: rotación en runtime sobre la misma Activity, sin Activity nueva (HU-019)
Se descarta una `Activity` separada para el visor (más complejidad de navegación/DI). En su lugar, al entrar a la ruta del visor se ajusta `activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR` en un `DisposableEffect`, restaurando `SCREEN_ORIENTATION_PORTRAIT` al salir — mismo patrón ya usado en `MainActivity.HighPriorityBackHandler` (acceso directo a la `ComponentActivity` vía `LocalContext.current as ComponentActivity`). Zoom vía `Modifier.pointerInput { detectTransformGestures { ... } }` (Compose Foundation, sin dependencia nueva) + `HorizontalPager` (`androidx.compose.foundation.pager`, ya incluido en Compose) para navegar entre imágenes.

## Risks / Trade-offs

- [Riesgo] La query combinada de filtros (decisión 5) puede degradar si se agregan muchos parámetros opcionales sin índices → Mitigación: índices en `Operation` sobre `assetId`, `resultado`, `dateTime` si el `EXPLAIN QUERY PLAN` real (fase `data`) muestra full scan con dataset sintético de 5,000 filas.
- [Riesgo] `PagingData.filter`/`insertSeparators` encadenados pueden interactuar de forma no obvia (orden de aplicación) → Mitigación: verificar con test instrumentado real (no solo unitario) que el filtro de `pendingDeleteIds` se aplica después de los separadores, para no dejar un `DayHeader` huérfano cuando su única operación está pendiente de borrado.
- [Riesgo] Soft-delete solo en memoria (decisión 4) no sobrevive rotación de configuración sin cuidado → Mitigación: `pendingDeleteIds` y el `Job` viven en el `ViewModel` (sobrevive rotación por `viewModelScope`), no en el Composable.
- [Riesgo] `LIKE` sin índice FTS puede ser lento en 5,000 filas con 4 columnas de texto → Mitigación aceptada explícitamente por la propia HU-023 (negociable); se revisa en la fase `data` de la sub-slice de filtros con el dataset sintético de HU-016.
