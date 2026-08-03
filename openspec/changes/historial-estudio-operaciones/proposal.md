## Why

Hoy todas las operaciones registradas (EP-001) quedan guardadas en Room pero no hay ninguna forma de revisarlas: no existe pantalla de Historial funcional (EP-005 dejó solo el placeholder de navegación). Sin poder repasar el historial agrupado, filtrarlo y estudiar el detalle de cada operación, el usuario no puede detectar los patrones de emociones/errores que son el objetivo #3 del PRD, ni confiar en que sus datos siguen accesibles con volumen real (3,000-5,000 operaciones, objetivo #2).

## What Changes

- Reemplaza el placeholder de `HistorialScreen` por el listado real agrupado por mes → día, orden más reciente primero, paginado con Paging 3 para mantener scroll fluido a escala (3,000-5,000 operaciones).
- Agrega resumen rápido por mes (n° operaciones, % ganadas, R acumulado) con colapsar/expandir por grupo.
- Agrega vista de detalle completo de una operación (secciones legibles, imagen primero), con editar (reutilizando el formulario de EP-001) y eliminar con deshacer vía snackbar (soft-delete transitorio, no requiere confirmación bloqueante).
- Agrega visor de imagen a pantalla completa (zoom, navegación entre imágenes de la misma operación, rotación excepcional a horizontal solo en este visor).
- Agrega filtros combinados (fecha/periodo, Activo, Resultado, Error, Emoción antes/después) con lógica AND, más búsqueda por palabra clave combinada con esos filtros.
- Persiste el último filtro relevante entre sesiones (DataStore Preferences).
- Agrega estados vacíos diferenciados: sin ninguna operación registrada vs. sin resultados para el filtro/búsqueda actual.

## Capabilities

### New Capabilities
- `historial-listado`: listado de Historial agrupado por mes/día, paginado (Paging 3) para scroll fluido a escala, tarjeta compacta por operación, resumen rápido por mes con colapsar/expandir, y estados vacíos (HU-015, HU-016, HU-017, HU-025).
- `historial-detalle-operacion`: vista de detalle completo desde la tarjeta, editar una operación existente, eliminar con deshacer vía snackbar (HU-018, HU-020, HU-021).
- `historial-visor-imagen`: visor de imagen a pantalla completa con zoom, navegación entre imágenes y rotación excepcional a horizontal (HU-019).
- `historial-filtros`: filtros combinados por etiqueta/fecha con lógica AND, búsqueda por palabra clave, y persistencia del último filtro entre sesiones (HU-022, HU-023, HU-024).

### Modified Capabilities
(ninguna — no se cambian requirements de capabilities existentes; `operation-entry-form` se reutiliza tal cual para editar, sin alterar su contrato)

## Impact

- **Código nuevo**: `feature/historial/` (HistorialScreen real reemplazando el placeholder de EP-005, HistorialViewModel(s), Paging3 `PagingSource`/`RemoteMediator` no aplica -- todo es local/Room, así que `PagingSource` directo sobre Room basta), visor de imagen a pantalla completa, filtros/búsqueda.
- **Datos**: nuevas queries en `OperationDao` (listado agrupado paginado, resumen por mes, filtros combinados), posible entidad/DAO de preferencias vía DataStore para el filtro persistido.
- **Reutiliza sin modificar**: `OperationFormScreen`/`OperationFormViewModel` (EP-001, para editar), `CatalogItemDao`/`CatalogRepository` (EP-002, para poblar los selectores de filtro por etiqueta), `ImageProcessor.thumbnailPathFor` (EP-001, para las miniaturas del listado y el visor).
- **Dependencias externas**: Paging 3 (`androidx.paging:paging-compose`) — verificar contra `stack-allowlist.json`/PRD técnico antes de agregarla (gate `stack-guard.sh`).

## Trazabilidad
- Épica: EP-003
- Historias: HU-015, HU-016, HU-017, HU-018, HU-019, HU-020, HU-021, HU-022, HU-023, HU-024, HU-025
- Discovery: docs/03-backlog/epicas.md#ep-003
