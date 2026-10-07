## ADDED Requirements

### Requirement: Alternancia porcentaje/número en el resumen de Inicio
El sistema SHALL ofrecer un único control de modo (Porcentaje ↔ Número) que alterna
simultáneamente las tarjetas de resumen y la gráfica de barras de distribución de resultados de
Inicio entre porcentaje y conteo absoluto de operaciones por categoría (Ganadas/Perdidas/BE). El
modo persiste solo durante la sesión activa en la pantalla de Inicio (incluidos cambios de
periodo/filtro de fecha) y vuelve al default (Porcentaje) al reabrir la app — no se persiste en
DataStore.

#### Scenario: Activar modo número
- **WHEN** el usuario, viendo el resumen en modo Porcentaje (p. ej. "Ganadas: 60%"), activa el
  control de alternancia a modo Número
- **THEN** el sistema muestra el conteo absoluto de operaciones por categoría (p. ej. "Ganadas: 9")
  simultáneamente en las tarjetas de resumen y en la gráfica de barras, sin recargar la pantalla

#### Scenario: Volver a modo porcentaje
- **WHEN** el usuario, con el resumen en modo Número, vuelve a activar el control a modo Porcentaje
- **THEN** el sistema restaura el porcentaje en ambas superficies (tarjetas y gráfica de barras) de
  forma consistente con el valor mostrado antes de alternar

#### Scenario: Periodo sin operaciones
- **WHEN** el usuario alterna a modo Número con un periodo seleccionado que no tiene operaciones
  registradas
- **THEN** el sistema muestra 0 en cada categoría (Ganadas, Perdidas, BE) sin errores, valores
  negativos ni "NaN"

#### Scenario: Riesgo promedio no se ve afectado por el modo
- **WHEN** el resumen está en modo Número y el usuario revisa la tarjeta "Riesgo promedio"
- **THEN** el sistema la sigue mostrando siempre en porcentaje, sin equivalente numérico,
  independientemente del modo activo

### Requirement: Conteo absoluto por categoría en el cálculo de métricas
`OperationDao.metricsSummary()` y `OperationMetricsSummary` SHALL exponer el conteo absoluto exacto
de operaciones por categoría (`winCount`, `lossCount`, `breakEvenCount`) para el periodo
seleccionado, calculado en SQL — no derivado del porcentaje entero ya redondeado, para no perder
precisión.

#### Scenario: Conteo exacto disponible para el alternador
- **WHEN** `InicioViewModel` solicita el resumen de métricas para el periodo seleccionado
- **THEN** `OperationMetricsSummary` incluye `winCount`, `lossCount` y `breakEvenCount` calculados
  directamente en SQL, consistentes con `operationCount` (su suma) y con los porcentajes ya
  existentes
