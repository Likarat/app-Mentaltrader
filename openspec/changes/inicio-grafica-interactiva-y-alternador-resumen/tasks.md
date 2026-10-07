## 1. Conteo absoluto por categoría (HU-038, data)

- [x] 1.1 ~~Test JVM~~ CORREGIDO en ejecución (tasks.md original asumía mal el arnés de testing):
      `metricsSummary()` es agregación SQL real (Room) — este proyecto NO tiene Robolectric/Room-en-JVM
      (ver KDoc de `FakeOperationDao.metricsSummary`, ya lanzaba `UnsupportedOperationException` antes
      de este change). La verificación de `winCount`/`lossCount`/`breakEvenCount` por SQL real queda
      en 1.1-bis, instrumentada, PENDIENTE de dispositivo/emulador conectado.
- [x] 1.2 Ampliar el SQL de `metricsSummary()` con las 3 columnas de conteo (mismo patrón
      `CASE WHEN`/`COALESCE` ya usado) (green, compila).
- [x] 1.3 Ampliar `OperationMetricsSummary` (data class) con los 3 campos nuevos.
- [x] 1.4 Actualizar `FakeOperationDao.kt` (test util) para reflejar los campos nuevos sin romper
      tests existentes de Inicio.
- [x] 1.5-bis `connectedDebugAndroidTest` real (dispositivo motorola edge 50 fusion, Room en memoria,
      cero riesgo sobre datos reales): `OperationMetricsCountsInstrumentedTest`, 3 tests verdes --
      `winCount+lossCount+breakEvenCount == operationCount`, consistentes con los porcentajes
      existentes (6/2/1 vs 67%/22%/11% con 9 operaciones), y en 0 sin errores con un periodo sin
      operaciones (Escenario 3 de HU-038).

## 2. Estado de modo Porcentaje/Número (HU-038, presentación)

- [x] 2.1 Test: `InicioViewModel` expone un `StateFlow<ResumenDisplayMode>` inicializado en
      `PORCENTAJE`, con función para alternar (red → green, `InicioViewModelTest`, 3 tests nuevos).
- [x] 2.2 Alternar el modo es una derivación local (`MutableStateFlow.toggled()`) sin flatMapLatest
      sobre `operationDao` — no dispara ninguna consulta nueva (verificado por lectura del código:
      `resumenDisplayMode` no participa de ningún `flatMapLatest`/`filterState`).
- [x] 2.3 `MetricsSummaryCards` lee el modo compartido y renderiza porcentaje o conteo en
      "% Ganadas"/"% Perdidas"/"% BE" según corresponda (Escenarios 1 y 2).
- [x] 2.4 "Riesgo promedio" ignora el modo: siempre en porcentaje (Escenario 4) — `MetricCard` de
      Riesgo promedio no recibe `mode` como parámetro (garantía estructural, no solo test).
- [x] 2.5 `ResultDistributionBarChart` recibe el modo y alterna la ETIQUETA de sus 3 barras
      (`ResultBar`) entre porcentaje y conteo absoluto; la ALTURA sigue atada al porcentaje siempre
      (design.md).
- [x] 2.6 Control de alternancia (`ResumenDisplayModeToggleRow`, `FilterChip`) visible en
      `InicioScreen`, conectado a `viewModel.resumenDisplayMode`/`toggleResumenDisplayMode`.

## 3. Tooltip interactivo de R acumulado (HU-037)

- [x] 3.0 **Ajuste de alcance descubierto en TDD** (no estaba en el proposal original):
      `ResultInRCumulativeSeries.Point` NO tenía `dateTime` — imposible mostrar "la fecha del punto"
      (AC Escenario 1) sin ampliarlo. Se amplió `OperationDao.resultInROrderedByDateAsc` (ahora
      `OperationDateResult(dateTime, resultInR)` en vez de `Float?` suelto) + `Point` + `build()` +
      `FakeOperationDao` + los 2 fakes inline de Historial (solo firma, sin comportamiento nuevo).
      Suite completa verde tras el cambio (`testDebugUnitTest` sin regresiones).
- [x] 3.1 Test JVM puro de la función de mapeo touchX→índice de punto más cercano (sin Compose):
      `RAcumuladoHitTestTest`, 7 tests, casos límite incluidos (antes del primero, después del
      último, exactamente 2 puntos, un único punto — Escenarios 4 y 5) (red → green).
- [x] 3.2 Integrado `Modifier.pointerInput`/`detectDragGestures` sobre el `Canvas` de
      `RAcumuladoLineChart`, usando `RAcumuladoHitTest.nearestIndex` y el `stepX`/escala ya
      existente.
- [x] 3.3 Indicador (línea vertical + tooltip con fecha + `InicioFormatting.signedR`) sobre el punto
      resaltado, actualizado en cada evento de arrastre (Escenarios 1 y 2) — por lectura de código;
      verificación visual real diferida a 4.2 (requiere dispositivo).
- [x] 3.4 El indicador permanece fijo tras soltar el dedo (no hay `onDragEnd` que lo limpie,
      Escenario 3) hasta un nuevo arrastre o un toque simple sobre el propio Canvas
      (`detectTapGestures`) — nota: "tocar FUERA de la gráfica" (en otra parte de la pantalla) NO
      quedó cableado, solo el toque simple dentro del propio Canvas; declarado como desviación menor
      del AC, a decidir si se amplía o se acepta así en el Release Gate.
- [x] 3.5 Clamping en los bordes cubierto por `RAcumuladoHitTest.nearestIndex` (coerceIn), mismo
      código que dibuja la polilínea real — verificación del gesto real (no solo de la función pura)
      diferida al instrumentado (4.3).

## 4. Journey smoke + fidelidad visual

- [x] 4.1 Journey-smoke de Inicio con ambas features activas verificado: `InicioFidelityScreenshotTest`
      monta `InicioScreen` real con 9 operaciones sembradas, alterna modo Porcentaje/Número (screenshot),
      arrastra sobre la gráfica de R acumulado (screenshot) -- `InicioViewModelTest` ya cubre en JVM
      que el modo NO se resetea al cambiar de periodo (solo al reabrir la app/reconstruir el ViewModel).
- [x] 4.2 Verificación visual REAL con dispositivo conectado (motorola edge 50 fusion):
      `fidelity_01_modo_porcentaje.png`, `fidelity_02_modo_numero.png`, `fidelity_03_tooltip_racumulado.png`.
      Hallazgo real detectado y corregido: el tooltip quedaba centrado fijo en vez de seguir el punto
      resaltado -- gate `fidelity` cierra tras el fix y la re-verificación visual.
- [x] 4.3 `connectedDebugAndroidTest` del gesto de arrastre sobre dispositivo real:
      `RAcumuladoDragGestureTest`, 7 tests verdes. Suite completa del proyecto: 57/57 verde en el
      dispositivo (1 test de EP-002 flaky bajo carga de suite completa, 5/5 verde en aislamiento, no
      relacionado a este slice).

## 5. Cierre

- [ ] 5.1 `openspec validate inicio-grafica-interactiva-y-alternador-resumen --strict`.
- [ ] 5.2 Back-references: nota `> OpenSpec change:
      inicio-grafica-interactiva-y-alternador-resumen` en `docs/03-backlog/epicas.md#ep-004` y en
      HU-037/HU-038.
- [ ] 5.3 Agregar HU-037/HU-038 a `docs/03-backlog/backlog.md` (gap de documentación detectado en
      DoR, no bloqueante pero a regularizar antes de cerrar).
- [ ] 5.4 Verificación adversarial independiente del cableado (`wiring-adversarial-verifier`) antes
      de cerrar `wiring_verified`/`dod`.
