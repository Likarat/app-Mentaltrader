## 1. Sub-slice EP-004-a: núcleo de métricas (resumen numérico, gráficas) + ranking de emociones/errores (HU-026, HU-027)

- [x] 1.1 `OperationDao.metricsSummary()`: agregación en SQL de todas las operaciones (conteo, % ganadas/perdidas/BE, calidad promedio, R total, R promedio) -- `CASE`/`COALESCE` evitan división por cero y `NULL` con cero operaciones (HU-026 Escenario 4). `design.md` decisión #2: sin `dateFrom`/`dateTo` todavía (EP-004-a asume "todas las operaciones", el selector real llega en EP-004-b).
- [x] 1.2 `OperationMetricsSummary` (`core/data`, nuevo data class) -- transporta lo ya calculado en SQL, sin recalcular nada (mismo criterio que `MonthSummary`).
- [x] 1.3 `OperationDao.resultInROrderedByDateAsc()`: proyección liviana de una sola columna (`Flow<List<Float?>>`), ordenada cronológicamente ascendente -- NO la entidad `Operation` completa (design.md decisión #3: SQLite de minSdk 27 no garantiza funciones de ventana, el punto-a-punto se resuelve en Kotlin puro, no en SQL).
- [x] 1.4 `ResultInRCumulativeSeries` (`feature/inicio/`, función pura): transforma la lista ordenada de `resultInR` en los puntos punto-a-punto del R acumulado (HU-026 Escenario 2), tratando `null` como `0` sin romper la serie (Escenario 4).
- [x] 1.5 `OperationDao.emotionRanking()`/`errorRanking()`: `GROUP BY`/`JOIN` en SQL contra `catalog_item`, desempate por nombre ascendente (HU-027 Escenario 3). `emotionRanking` cuenta AMBOS roles (`emotionBeforeId`/`emotionAfterId`) vía `UNION ALL` antes de agrupar (design.md decisión #4). `CatalogRankingItem` (`core/data`, nuevo data class) transporta id/nombre/frecuencia ya resueltos.
- [x] 1.6 `InicioViewModel` nuevo (`feature/inicio/`): expone `metricsSummary`/`emotionRanking`/`errorRanking` como pass-through directos de `OperationDao` (mismo criterio que `HistorialViewModel.monthSummaries`/`assets`, envueltos en `flow { emitAll(...) }`) y `cumulativeSeries` (única transformación real del ViewModel, vía `ResultInRCumulativeSeries.build`).
- [x] 1.7 `InicioScreen` real (`feature/inicio/`, reemplaza el placeholder movido desde `feature/metricas/`): tarjetas de resumen numérico, gráfica de línea de R acumulado y gráfica de barras de distribución (`InicioCharts.kt`, Compose Canvas puro -- design.md decisión #1, sin librería nueva), y las 2 secciones de ranking (emociones, errores). Colores verde/rojo para valores con signo (`InicioFormatting.colorFor`), formateo determinístico con `Locale.US` (mismo criterio que `MonthSummaryFormatting`).
- [x] 1.8 Cableado real: `MainActivity.kt` reemplaza `InicioScreen()` (placeholder) por `InicioViewModel`/`InicioScreen` reales, vía `InicioViewModel.Factory(application.database.operationDao())` -- misma ruta `Destino.Inicio.route` ya existente (EP-005), sin cambios de navegación.
- [x] 1.9 Tests unitarios: `ResultInRCumulativeSeriesTest` (6 tests: vacío, un valor, varios valores acumulando en orden, valor `null` no rompe la serie, todos `null` da serie plana en 0, acumulado puede terminar negativo) + `InicioViewModelTest` (3 tests: sin operaciones la serie queda vacía, la serie refleja el R de cada operación en orden cronológico real -- no el de inserción --, un valor `null` no rompe la serie) -- `testDebugUnitTest` (176 tests en toda la suite) + `assembleDebug` + `compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL, sin regresiones. `metricsSummary`/`emotionRanking`/`errorRanking` NO se ejercitan a nivel de `InicioViewModel` (agregación SQL real, mismo criterio ya establecido para `HistorialViewModel.monthSummaries` -- diferido al instrumentado de 1.10).
- [ ] 1.10 Test instrumentado con Room real: verificar `metricsSummary`/`emotionRanking`/`errorRanking` contra un dataset sembrado (incluyendo el caso de cero operaciones y el de una operación con `resultInR = null`, para confirmar el comportamiento real del warning de KSP documentado en `design.md` Riesgos) -- diferido a la pasada final de instrumentados (sin dispositivo conectado en esta sesión, mismo criterio ya usado en EP-003/EP-005).
- [ ] 1.11 `journey_smoke` EP-004-a: abrir Inicio con operaciones reales de distintos resultados/emociones/errores y ver las tarjetas, gráficas y rankings reales -- diferido a la pasada final de instrumentados, misma razón que 1.10.

## 2. Sub-slice EP-004-b: selector de periodo predefinido (HU-028)

- [ ] 2.1 Extender `OperationDao.metricsSummary()`/`resultInROrderedByDateAsc()`/`emotionRanking()`/`errorRanking()` con `dateFrom`/`dateTo` opcionales (mismo patrón evolutivo que `pagingSourceOrderedByDateDesc` -> `pagingSourceFiltered` en EP-003, design.md decisión #2) -- todas siguen devolviendo "todas las operaciones" cuando ambos son `null`.
- [ ] 2.2 `PeriodoInicioFiltro`/rango resuelto (función pura, análoga a `PeriodoFiltro`/`PeriodoFiltroRange` de `feature/historial`) para Día/Semana/Mes/Últimos 3 meses.
- [ ] 2.3 `InicioViewModel` gana el periodo seleccionado (`StateFlow`) y reconstruye las 4 queries vía `flatMapLatest` cada vez que cambia (HU-028 Escenario 2: recalcula desde cero, sin arrastrar datos del periodo anterior).
- [ ] 2.4 UI del selector fijo en la parte superior de `InicioScreen` (HU-028 Escenario 3: permanece visible al hacer scroll -- fuera del `LazyColumn` de contenido).
- [ ] 2.5 Tests unitarios (JVM, sin Room): `PeriodoInicioFiltroRangeTest` + `InicioViewModelTest` (recalcular al cambiar de periodo, aislamiento entre periodos sucesivos).
- [ ] 2.6 Test instrumentado + `journey_smoke` EP-004-b: seleccionar cada periodo predefinido real y verificar el recálculo -- diferido a la pasada final de instrumentados.

## 3. Sub-slice EP-004-c: estado vacío en Inicio (HU-031)

- [ ] 3.1 `InicioViewModel` expone el conteo total de operaciones (reutiliza `OperationDao.countAll()`, ya existente desde EP-003) para distinguir "primera vez" de "sin datos para el periodo".
- [ ] 3.2 `InicioScreen` reutiliza `HistorialEmptyState`/`HistorialEmptyStateContent` (EP-003, HU-025) para el caso de "primera vez, nunca registró ninguna operación" (HU-031 Escenario 1) -- sin duplicar el mensaje/lógica, mismo componente que Historial.
- [ ] 3.3 Mensaje propio de "sin datos para el periodo seleccionado" (HU-031 Escenario 2), sin acceso directo de creación (las operaciones sí existen, solo no en este rango).
- [ ] 3.4 Verificar recuperación fluida al cambiar a un periodo con datos (HU-031 Escenario 3): el estado vacío desaparece sin rastros al recibir datos reales del nuevo periodo.
- [ ] 3.5 Tests unitarios + test instrumentado/`journey_smoke` EP-004-c -- diferidos según corresponda (unitarios en esta sesión si no dependen de Room/selector real; instrumentado a la pasada final).

## 4. Sub-slice EP-004-d: rango de fechas personalizado + persistencia del filtro de periodo (HU-029, HU-030)

- [ ] 4.1 UI de calendario para elegir fecha de inicio/fin al seleccionar "Personalizado" (HU-029 Escenario 1), con validación de rango inválido (fin anterior a inicio, Escenario 3).
- [ ] 4.2 `InicioViewModel` recalcula sobre el rango personalizado confirmado (HU-029 Escenario 2), mismo mecanismo que el periodo predefinido de EP-004-b.
- [ ] 4.3 `InicioFilterRepository` (`core/data`, DataStore Preferences, nuevo -- claves propias de Inicio, independientes de `HistorialFilterRepository`) persiste el último filtro de periodo (predefinido o rango personalizado) -- mismo patrón que HU-024/EP-003.
- [ ] 4.4 `InicioViewModel` restaura el filtro persistido en `init` (HU-030 Escenario 1/2) y aplica un periodo por defecto razonable si nunca hubo uno persistido (Escenario 3).
- [ ] 4.5 Tests unitarios (`InicioFilterRepositoryTest` con DataStore real de prueba, mismo patrón que `HistorialFilterRepositoryTest`) + test instrumentado/`journey_smoke` EP-004-d -- diferidos según corresponda.

## 5. Cierre del change completo

- [ ] 5.1 Suite completa (`testDebugUnitTest` + `connectedDebugAndroidTest`) en verde con evidencia real de ejecución, sin regresiones sobre EP-001/EP-002/EP-003/EP-005.
- [ ] 5.2 `wiring_checklist` completo sin ítems `failing` no justificados.
- [ ] 5.3 `openspec validate metricas-deteccion-patrones-inicio --strict`.
- [ ] 5.4 Back-reference: agregar `openspec_change: metricas-deteccion-patrones-inicio` en `docs/03-backlog/epicas.md#ep-004` y en el frontmatter/nota de cada una de las 6 HU (HU-026 a HU-031).
