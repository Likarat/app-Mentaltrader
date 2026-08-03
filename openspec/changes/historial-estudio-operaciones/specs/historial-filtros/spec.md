## ADDED Requirements

### Requirement: Filtrar por etiqueta y por fecha/periodo con lógica AND
El sistema SHALL permitir filtrar el historial por Activo, Resultado, Error, Emoción antes, Emoción después, y por fecha/periodo (última semana, último mes, este mes, últimos 3 meses, o rango personalizado), combinando varios filtros simultáneamente con lógica AND, y SHALL permitir limpiar todos los filtros a la vez.

#### Scenario: Filtrar por una etiqueta
- **WHEN** el usuario está en Historial y aplica un filtro por Activo, Emoción, Error o Resultado
- **THEN** el sistema muestra solo las operaciones que coincidan, manteniendo el agrupamiento por mes

#### Scenario: Filtrar por fecha o periodo
- **WHEN** el usuario está en Historial y selecciona una fecha específica, un periodo predefinido, o un rango personalizado
- **THEN** el sistema muestra solo las operaciones dentro de ese rango

#### Scenario: Combinar múltiples filtros de etiqueta con lógica AND
- **WHEN** el usuario aplicó un filtro de Activo y un filtro de Resultado al mismo tiempo
- **THEN** el sistema muestra solo las operaciones que cumplen ambos criterios simultáneamente, no solo uno de ellos

#### Scenario: Limpiar todos los filtros
- **WHEN** el usuario tiene uno o más filtros activos y toca "Limpiar filtros"
- **THEN** el sistema remueve todos los filtros y muestra el listado completo agrupado por mes

### Requirement: Búsqueda por palabra clave combinada con los filtros
El sistema SHALL permitir buscar por palabra clave (coincidencia parcial) sobre `entryDescription`, `emotionBeforeReason`, `emotionAfterReason` y `errorReason`, combinando esa búsqueda con los filtros de etiqueta/fecha activos mediante lógica AND.

#### Scenario: Buscar por palabra clave
- **WHEN** el usuario está en Historial e ingresa una palabra clave en el buscador
- **THEN** el sistema muestra las operaciones cuyos campos de texto (`entryDescription`, `emotionBeforeReason`, `emotionAfterReason`, `errorReason`) contengan esa palabra

#### Scenario: Combinar búsqueda de texto con un filtro de etiqueta activo
- **WHEN** el usuario aplicó el filtro Activo = "XAUUSD" y adicionalmente escribe una palabra clave en el buscador
- **THEN** el sistema muestra solo operaciones de ese Activo cuyos campos de texto contengan esa palabra clave

#### Scenario: Búsqueda sin coincidencias
- **WHEN** el usuario ingresa una palabra clave que no coincide con ningún campo de texto de ninguna operación
- **THEN** el sistema muestra el listado vacío para ese criterio, sin error

### Requirement: Persistir el último filtro entre sesiones
El sistema SHALL guardar en DataStore Preferences el último estado de filtros y búsqueda aplicado en Historial, y SHALL restaurarlo automáticamente al reabrir la app, incluso tras un reinicio completo del proceso.

#### Scenario: Restaurar filtros al reabrir Historial
- **WHEN** el usuario aplicó filtros en Historial, cierra la app y la reabre
- **THEN** el sistema restaura el último estado de filtros guardado

#### Scenario: Persistencia real entre reinicios completos del proceso
- **WHEN** el usuario aplicó filtros y cerró completamente la app (proceso terminado, no solo en segundo plano) y la vuelve a abrir
- **THEN** los filtros siguen restaurados, confirmando que la persistencia es en disco y no solo en memoria de sesión

#### Scenario: Primera vez sin filtros previos
- **WHEN** el usuario nunca aplicó ningún filtro en Historial y lo abre por primera vez
- **THEN** el sistema lo muestra sin ningún filtro activo, con el comportamiento por defecto
