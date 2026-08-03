## ADDED Requirements

### Requirement: Estado vacío en Inicio
El sistema SHALL distinguir, en Inicio, entre "nunca hubo operaciones registradas" (reutilizando el mismo mensaje y acceso directo ya definidos para Historial) y "hay operaciones pero ninguna cae dentro del periodo seleccionado" (sin ese acceso directo), y SHALL recuperarse de forma fluida al cambiar a un periodo con datos.

#### Scenario: Primera vez sin operaciones registradas (reutiliza el estado vacío de Historial)
- **WHEN** el usuario no ha registrado ninguna operación aún y abre Inicio
- **THEN** el sistema muestra el mismo mensaje amigable y acceso directo al formulario ya definidos para el estado vacío de Historial, sin redefinir su contenido

#### Scenario: Sin datos para el periodo seleccionado
- **WHEN** el usuario tiene operaciones registradas, pero ninguna cae dentro del periodo actualmente seleccionado, y la pantalla de Inicio se renderiza
- **THEN** el sistema muestra un estado vacío indicando que no hay datos para ese periodo, sin ofrecer el acceso directo de creación

#### Scenario: Recuperación fluida al cambiar a un periodo con datos
- **WHEN** el sistema muestra el estado vacío de "sin datos para este periodo" y el usuario selecciona un periodo distinto que sí tiene operaciones
- **THEN** el sistema muestra el panel de métricas normalmente, sin rastros del estado vacío anterior
