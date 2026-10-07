## Why

Las tarjetas de resumen y la gráfica de R acumulado de Inicio (HU-026, ya en producción) solo
muestran valores agregados: el trader ve "% Ganadas: 60%" o la forma general de la curva de R, pero
no puede ver el valor exacto detrás de un punto concreto de la curva, ni saber cuántas operaciones
concretas respaldan un porcentaje sin ir a Historial a contarlas. HU-037 y HU-038 son dos mejoras
post-construcción pedidas directamente por el usuario sobre el producto ya en uso, para cerrar esa
brecha de exploración de datos sin salir de Inicio.

## What Changes

- Tocar y arrastrar sobre la gráfica de R acumulado (Canvas) muestra la fecha y el valor de R del
  punto más cercano al dedo, con un indicador visual (línea/punto resaltado) que sigue el gesto.
- Al soltar el gesto, el indicador desaparece y la gráfica vuelve a su estado normal.
- Un único control de modo (Porcentaje ↔ Número) alterna simultáneamente las tarjetas de resumen y
  la gráfica de barras de distribución de resultados entre porcentaje y conteo absoluto de
  operaciones por categoría (Ganadas/Perdidas/BE).
- `OperationDao.metricsSummary()` y `OperationMetricsSummary` se amplían para exponer el conteo
  absoluto exacto por categoría (`winCount`/`lossCount`/`breakEvenCount`), además de los porcentajes
  ya existentes — necesario porque derivar el conteo del porcentaje entero redondeado perdería
  precisión.
- La métrica "Riesgo promedio" se mantiene siempre en porcentaje, sin equivalente numérico, sin que
  el control de modo la afecte.
- El modo (Porcentaje/Número) persiste mientras el usuario permanece en Inicio (incluye cambios de
  periodo/filtro de fecha) pero vuelve al default (Porcentaje) al reabrir la app — decisión de
  producto confirmada explícitamente con el usuario (no usa DataStore, a diferencia del filtro de
  periodo de HU-030).

## Capabilities

### New Capabilities
- `inicio-grafica-r-interactiva`: exploración táctil (tocar/arrastrar) sobre la gráfica de R
  acumulado de Inicio, mostrando fecha y valor de R del punto más cercano al gesto (HU-037).
- `inicio-alternador-resumen-numerico`: control único que alterna tarjetas de resumen y gráfica de
  distribución de resultados de Inicio entre porcentaje y conteo absoluto por categoría (HU-038).

### Modified Capabilities
(ninguna — no existe aún una capability sincronizada en `openspec/specs/` para el resumen de Inicio
de HU-026: el change `metricas-deteccion-patrones-inicio` que la introdujo sigue sin archivar, con
sus delta-specs en `openspec/changes/metricas-deteccion-patrones-inicio/specs/` pendientes de sync.
Esta deuda queda documentada aparte, no bloquea este change.)

## Impact

- `app/src/main/java/com/miguel/mentaltrader/feature/inicio/InicioCharts.kt` — gesto de
  tocar/arrastrar sobre el Canvas de la gráfica de R acumulado; gráfica de barras con modo
  porcentaje/número.
- `app/src/main/java/com/miguel/mentaltrader/feature/inicio/InicioScreen.kt` — control de
  alternancia de modo; tarjetas de resumen (`MetricsSummaryCards`/`MetricCard`) leyendo el modo
  compartido.
- `app/src/main/java/com/miguel/mentaltrader/feature/inicio/InicioViewModel.kt` — estado de modo
  (Porcentaje/Número) de solo-sesión (sin DataStore).
- `app/src/main/java/com/miguel/mentaltrader/core/data/OperationDao.kt` — amplía
  `metricsSummary()` (SQL) para exponer conteo absoluto por categoría.
- Data class `OperationMetricsSummary` — nuevos campos `winCount`/`lossCount`/`breakEvenCount`.
- `app/src/test/java/com/miguel/mentaltrader/testutil/FakeOperationDao.kt` — fake usado en tests
  existentes de Inicio, debe reflejar los campos nuevos.
- Sin cambios de dependencias ni de API externa (app nativa sin backend).

## Trazabilidad
- Épica: EP-004
- Historias: HU-037, HU-038
- Discovery: docs/03-backlog/epicas.md#ep-004--métricas-y-detección-de-patrones-inicio
