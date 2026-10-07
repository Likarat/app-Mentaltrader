## Context

`InicioCharts.kt` ya dibuja la gráfica de R acumulado (`RAcumuladoLineChart`) y la gráfica de
distribución (`ResultDistributionBarChart`) con Compose `Canvas` puro, sin ninguna librería de
gráficas (`stack-allowlist.json` no admite una hoy). `RAcumuladoLineChart` calcula `stepX = size.width
/ (points.size - 1)` y una función `yFor(value)` para mapear puntos a coordenadas de pantalla —
ambas se reutilizan tal cual para el hit-testing del arrastre (HU-037), invirtiendo el mapeo: de
posición X de pantalla a índice de punto más cercano.

`MetricsSummaryCards` (en `InicioScreen.kt`) y `ResultDistributionBarChart` leen hoy exclusivamente
los porcentajes de `OperationMetricsSummary` (`winPercent`/`lossPercent`/`breakEvenPercent`,
enteros ya redondeados en SQL). No existe un conteo absoluto por categoría — derivarlo del
porcentaje entero perdería precisión por redondeo (p. ej. 33%/33%/33% con 9 operaciones no permite
reconstruir si fueron 3/3/3 o 3/3/3 con empates distintos tras redondeo). HU-038 requiere ampliar
el cálculo SQL (`OperationDao.metricsSummary()`) para exponer el conteo exacto.

## Goals / Non-Goals

**Goals:**
- Tooltip interactivo (tocar/arrastrar) sobre `RAcumuladoLineChart`, sin nueva dependencia.
- Un control de modo compartido (Porcentaje/Número) que afecta `MetricsSummaryCards` y
  `ResultDistributionBarChart` a la vez.
- Conteo absoluto exacto por categoría calculado en SQL, no derivado del porcentaje redondeado.

**Non-Goals:**
- No se agrega ninguna librería de gráficas (sigue prohibido por `stack-allowlist.json`).
- No se persiste el modo Porcentaje/Número entre sesiones (decisión de producto confirmada con el
  usuario: 2026-10-07, ver HU-038 §Contexto) — a diferencia del filtro de periodo (HU-030), que sí
  usa DataStore.
- No se reabre ni se toca el scope viejo de EP-004 (HU-026..031): su deuda de cierre
  (`wiring_verified`/`dod`) queda exactamente igual de abierta que antes de este change.
- No se cambia la serie `ResultInRCumulativeSeries.Point` ni su cálculo (HU-037 solo agrega una
  forma de leerla interactivamente).

## Decisions

### Decisión 1 — Hit-testing manual reutilizando la escala existente, no una librería
`RAcumuladoLineChart` ya expone (localmente) `stepX` y el rango `[minValue, maxValue]` usado para
dibujar. El tooltip invierte ese mismo cálculo: `index = round(touchX / stepX).coerceIn(0,
points.lastIndex)`. Se implementa con `Modifier.pointerInput(points) { detectDragGestures(...) }`
sobre el `Canvas` ya existente. Alternativa descartada: librería de gráficas con soporte de
tooltip nativo — prohibida por `stack-allowlist.json` y sobre-ingeniería para una polilínea simple
ya dibujada a mano.

### Decisión 2 — Estado del tooltip en el propio composable, no en el ViewModel
La posición del dedo y el índice resaltado son estado de interacción puramente de UI (no de
dominio), análogo a `showHelp` en `RAcumuladoTitleWithHelp` (ya en el archivo). Se modela con
`remember { mutableStateOf<Int?>(null) }` dentro de `RAcumuladoLineChart`, no en
`InicioViewModel`. Esto mantiene el ViewModel sin estado de presentación efímero y evita
recomposiciones innecesarias de pantallas hermanas.

### Decisión 3 — El modo Porcentaje/Número vive en `InicioViewModel` como `StateFlow`, no en DataStore
A diferencia de HU-030 (filtro de periodo, que sí persiste vía DataStore porque el usuario espera
recuperar su filtro al reabrir la app), el modo de visualización es una preferencia de exploración
momentánea — decisión de producto ya confirmada explícitamente. Se modela como un
`MutableStateFlow<ResumenDisplayMode>` inicializado siempre en `PORCENTAJE`, compartido por
`MetricsSummaryCards` y `ResultDistributionBarChart` vía el mismo `InicioUiState` que ya exponen
ambas. Alternativa descartada: `rememberSaveable` a nivel de `InicioScreen` — se prefiere el
ViewModel porque el modo debe sobrevivir a cambios de periodo/filtro de fecha (recomposición del
`LazyColumn` completo), no solo a rotación.

### Decisión 4 — Conteo absoluto en el mismo SQL de `metricsSummary()`, no una query aparte
`winCount`/`lossCount`/`breakEvenCount` se agregan como columnas adicionales del mismo `SELECT`
agregado que ya calcula `winPercent` et al. (mismo `CASE WHEN`/`COALESCE` ya usado para evitar
división por cero), evitando una segunda query y manteniendo ambos valores (porcentaje y conteo)
atómicamente consistentes entre sí para el mismo periodo. `OperationMetricsSummary` gana los tres
campos nuevos; `FakeOperationDao` (test util) debe reflejarlos para no romper tests existentes de
Inicio.

### Decisión 5 — "Riesgo promedio" nunca cambia de modo
`MetricCard(label = "Riesgo promedio", value = "${avgRiskPercentage}%")` (línea ~337 de
`InicioScreen.kt`) no recibe el modo como parámetro: siempre renderiza con `%`. Es la única tarjeta
de `MetricsSummaryCards` que no lee del par porcentaje/conteo — no tiene equivalente numérico por
definición (es un promedio, no una categoría contable).

## Risks / Trade-offs

- **[Riesgo] Hit-testing con series muy cortas (2 puntos) podría producir `stepX` grande y mapeo
  impreciso** → Mitigación: el AC Escenario 5 de HU-037 exige explícitamente el caso de 2 puntos;
  se cubre con TDD directo sobre la función de mapeo antes de integrarla al gesto.
- **[Riesgo] Cambiar el modo mientras un arrastre de HU-037 está activo** (interacción cruzada no
  contemplada en ningún AC) → Mitigación: HU-037 y HU-038 son ortogonales (gráficas distintas:
  línea vs. barras/tarjetas); no hay AC que las combine, así que no se agrega lógica para ese
  cruce — si aparece un bug real de interacción se trata como hallazgo nuevo, no se infiere alcance
  no pedido.
- **[Riesgo] Ampliar `metricsSummary()` con 3 columnas nuevas podría afectar queries existentes que
  dependen de la forma de `OperationMetricsSummary`** → Mitigación: todo consumidor existente
  (`InicioScreen.kt`, `FakeOperationDao.kt`) se actualiza en el mismo change; se corre la suite JVM
  completa de Inicio antes de cerrar TDD, no solo los tests nuevos.

## Open Questions

Ninguna pendiente: la única decisión de producto abierta (persistencia del modo entre sesiones) ya
fue confirmada explícitamente por el usuario el 2026-10-07 (ver Non-Goals y HU-038 §Contexto).
