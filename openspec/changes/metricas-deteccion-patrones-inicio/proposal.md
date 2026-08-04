## Why

Hoy la pestaña "Inicio" (`InicioScreen`) es solo un placeholder de navegación (EP-005 dejó únicamente el destino cableado). Con EP-001/EP-002/EP-003 ya construidas, las operaciones y sus catálogos (Activo, Emociones, Errores) existen en Room, pero nadie los agrega ni los estudia: el usuario no tiene forma de ver de un vistazo cómo le está yendo (resumen numérico, R acumulado, distribución de resultados) ni de detectar, sin esfuerzo manual, qué emociones y errores se repiten más — que es el objetivo #3 del PRD y el propósito central del producto ("mejora continua" declarado en la Introducción del PRD).

## What Changes

- Reemplaza el placeholder de `InicioScreen` por la pantalla real de Inicio: tarjetas de resumen numérico (total de operaciones, % ganadas/perdidas/break even, calidad promedio, R total, R promedio), gráfica de línea de R acumulado y gráfica de barras de distribución de resultados (Compose Canvas puro, sin librería de gráficas nueva).
- Agrega el ranking de emociones y errores más frecuentes, calculado sobre el mismo periodo que las tarjetas/gráficas.
- Agrega el selector de periodo (Día/Semana/Mes/Últimos 3 meses/Personalizado), fijo en la parte superior, que recalcula todas las métricas/ranking del periodo elegido.
- Persiste el último filtro de periodo seleccionado entre sesiones (DataStore Preferences, mismo mecanismo ya usado por HU-024/EP-003).
- Agrega el estado vacío de Inicio cuando no hay operaciones en el periodo seleccionado (reutiliza el componente de estado vacío de HU-025/EP-003 sin duplicar lógica).

## Capabilities

### New Capabilities
- `inicio-resumen-metricas`: tarjetas de resumen numérico y las 2 gráficas (línea de R acumulado, barras de distribución de resultados) del periodo seleccionado (HU-026).
- `inicio-ranking-patrones`: ranking de emociones y errores más frecuentes del periodo seleccionado, con desempate determinístico (HU-027).
- `inicio-selector-periodo`: selector de periodo predefinido y personalizado, con recálculo real de métricas/ranking y persistencia entre sesiones (HU-028, HU-029, HU-030).
- `inicio-estado-vacio`: estado vacío diferenciado en Inicio cuando no hay operaciones para el periodo seleccionado (HU-031).

### Modified Capabilities
(ninguna — no se cambia el contrato de ninguna capability existente; se reutilizan sin modificar `OperationDao`/`CatalogItemDao` ya existentes -- solo se agregan queries/métodos nuevos -- y el componente de estado vacío de `historial-listado`, HU-025)

## Impact

- **Código nuevo**: `feature/inicio/` (reemplaza el placeholder de EP-005) — `InicioScreen`, `InicioViewModel`, gráficas Canvas (`InicioCharts.kt`), post-procesamiento puro del R acumulado (`ResultInRCumulativeSeries`), selector de periodo (`PeriodoInicioFiltro` o equivalente, sub-slices EP-004-b/d).
- **Datos**: nuevas queries de agregación en `OperationDao` (resumen numérico, serie de R por operación, ranking de emociones/errores) y un repositorio de persistencia del filtro de periodo (DataStore Preferences, `core/data`, análogo a `HistorialFilterRepository` de EP-003).
- **Reutiliza sin modificar**: `Operation`/`OperationDao` (EP-001), `CatalogItem`/`CatalogItemDao` (EP-002), el componente de estado vacío de Historial (`HistorialEmptyState`/`HistorialEmptyStateContent`, HU-025/EP-003).
- **Dependencias externas**: ninguna nueva — las gráficas se resuelven con Compose Canvas puro (`androidx.compose.foundation`, ya cubierto por `stack-allowlist.json`); `stack-allowlist.json` no admite hoy ninguna librería de gráficas (ver `design.md` decisión #1).

## Trazabilidad
- Epica: EP-004
- Historias: HU-026, HU-027, HU-028, HU-029, HU-030, HU-031
- Discovery: docs/03-backlog/epicas.md#ep-004
