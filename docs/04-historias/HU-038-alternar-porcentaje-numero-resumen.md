---
id: HU-038
titulo: Alternar entre porcentaje y número absoluto en el resumen de Inicio
epica: EP-004
prioridad: Should
complejidad: M
estado: lista
---

# Alternar entre porcentaje y número absoluto en el resumen de Inicio

## Historia

Como **trader de forex**,
quiero **alternar, con un único control, entre ver porcentaje y número absoluto de operaciones ganadas/perdidas/break even en el resumen de Inicio**,
para **entender de un vistazo cuántas operaciones concretas respaldan cada porcentaje, sin tener que ir a Historial a contarlas**.

## Contexto

Extiende **HU-026** (tarjetas de resumen numérico y gráfica de barras de distribución de resultados), que hoy solo expone `% Ganadas`, `% Perdidas` y `% BE`. Esta historia agrega un único control de modo (Porcentaje ↔ Número) que afecta **simultáneamente** las tarjetas de resumen y la gráfica de barras — ambas superficies cambian de modo a la vez, nunca de forma independiente. La métrica "Riesgo promedio" no tiene equivalente en número de operaciones (es un promedio de porcentaje de riesgo, no un conteo de categoría) y por lo tanto **se mantiene siempre en %**, sin que el control la afecte.

**Nota técnica de origen de datos**: `OperationMetricsSummary` (y el cálculo SQL en `OperationDao.metricsSummary` que lo alimenta) hoy solo expone `winPercent`, `lossPercent`, `breakEvenPercent` (enteros ya redondeados) y `operationCount` (total). No existe un conteo absoluto por categoría (cuántas ganadas, perdidas, BE por separado) — derivarlo del porcentaje entero perdería precisión por redondeo. Esta historia requiere ampliar el cálculo SQL y el data class para exponer el conteo exacto por categoría (p. ej. `winCount`, `lossCount`, `breakEvenCount`), no es un cambio solo de UI.

**Decisión de producto confirmada (2026-10-07, usuario)**: el modo elegido (Porcentaje/Número) persiste mientras el usuario permanece en la pantalla de Inicio —incluyendo cambios de periodo/filtro de fecha— pero vuelve al default (Porcentaje) al reabrir la app. No usa DataStore (a diferencia de **HU-030**, filtro de periodo, que sí persiste entre sesiones): se confirmó explícitamente mantener el alcance acotado a estado de sesión.

## Criterios de aceptación

### Escenario 1 — Happy path: activar modo número
- **Dado que** el usuario está en Inicio viendo el resumen en modo Porcentaje (ej. "Ganadas: 60%")
- **Cuando** activa el control de alternancia a modo Número
- **Entonces** el sistema muestra el conteo absoluto de operaciones por categoría (ej. "Ganadas: 9") simultáneamente en las tarjetas de resumen y en la gráfica de barras, sin recargar la pantalla

### Escenario 2 — Happy path: volver a modo porcentaje
- **Dado que** el usuario tiene el resumen en modo Número
- **Cuando** vuelve a activar el control de alternancia a modo Porcentaje
- **Entonces** el sistema restaura el porcentaje en ambas superficies (tarjetas y gráfica de barras) de forma consistente con el valor mostrado antes de alternar

### Escenario 3 — Edge: una categoría sin operaciones dentro de un periodo con datos
- **Dado que** el periodo seleccionado tiene operaciones, pero ninguna de una categoría concreta (p. ej. ninguna Perdida)
- **Cuando** el usuario alterna a modo Número
- **Entonces** el sistema muestra 0 en esa categoría (sin valores negativos ni "NaN"), mientras las demás categorías muestran su conteo real

> **Nota de alcance (confirmada con el usuario, 2026-10-07)**: un periodo **sin ninguna operación**
> no llega a mostrar este control — la pantalla cae en el estado vacío "sin datos para el periodo
> seleccionado" de HU-031 (que reemplaza todo el contenido, incluido el alternador). Este escenario
> queda acotado a "alguna categoría en cero dentro de un periodo que sí tiene operaciones"; el caso
> de periodo totalmente vacío es responsabilidad de HU-031, no de este control.

### Escenario 4 — Regla: Riesgo promedio no se ve afectado por el modo
- **Dado que** el resumen está en modo Número
- **Cuando** el usuario revisa la tarjeta "Riesgo promedio"
- **Entonces** el sistema la sigue mostrando siempre en porcentaje, sin un equivalente numérico, independientemente del modo activo

## Notas técnicas (opcional)

- Requiere ampliar `OperationDao.metricsSummary` (SQL) y `OperationMetricsSummary` (data class) para exponer conteo absoluto por categoría, no derivarlo del porcentaje redondeado.
- Afecta dos composables existentes de HU-026: `MetricsSummaryCards`/`MetricCard` y `ResultDistributionBarChart` (`InicioScreen.kt`, `InicioCharts.kt`) — ambos deben leer el mismo estado de modo compartido.

## Checklist INVEST

- [x] **I**ndependent — sí: extiende HU-026 agregando un control nuevo sobre datos que esa historia ya expone; no depende de HU-027/028/029/030/031 para implementarse.
- [x] **N**egotiable — el diseño exacto del control (switch, chip, ícono) es negociable; el AC fija el comportamiento, no el componente visual.
- [x] **V**aluable — sí: el porcentaje solo no deja ver si "60%" son 6 de 10 operaciones o 60 de 100, un trader necesita esa magnitud para calibrar confianza en el dato.
- [x] **E**stimable — alcance acotado: 1 cambio de origen de datos (DAO + data class) + 1 control de UI que conmuta 2 superficies ya existentes.
- [x] **S**mall — cabe en 2-4 días: no agrega pantallas nuevas, reutiliza composables existentes de HU-026.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (activar, desactivar, cero operaciones, regla de Riesgo promedio).

> OpenSpec change: `inicio-grafica-interactiva-y-alternador-resumen`.
