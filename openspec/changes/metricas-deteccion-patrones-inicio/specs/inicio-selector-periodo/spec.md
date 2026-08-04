## ADDED Requirements

### Requirement: Selector de periodo predefinido
El sistema SHALL ofrecer un selector de periodo predefinido (Día, Semana, Mes, Últimos 3 meses) fijo en la parte superior de Inicio, que recalcula el resumen numérico, las gráficas y el ranking usando solo las operaciones dentro de ese periodo.

#### Scenario: Recalcular métricas al seleccionar un periodo
- **WHEN** el usuario está en Inicio y selecciona un periodo (Día, Semana, Mes o Últimos 3 meses)
- **THEN** el sistema recalcula todas las métricas (tarjetas, gráficas, ranking) usando solo las operaciones dentro de ese periodo

#### Scenario: Cambiar de un periodo a otro sucesivamente
- **WHEN** el usuario ya tiene un periodo seleccionado con métricas calculadas y selecciona un periodo distinto
- **THEN** el sistema recalcula todas las métricas desde cero para el nuevo periodo, sin arrastrar datos del periodo anterior

#### Scenario: Selector fijo visible al hacer scroll
- **WHEN** el usuario hace scroll en la pantalla de Inicio
- **THEN** el selector de periodo permanece fijo y visible en la parte superior

### Requirement: Rango de fechas personalizado
El sistema SHALL permitir elegir "Personalizado" dentro del selector de periodo, mostrando un calendario para elegir fecha de inicio y fin, y SHALL validar que la fecha de fin no sea anterior a la de inicio.

#### Scenario: Elegir un rango personalizado
- **WHEN** el usuario está en Inicio y selecciona "Personalizado"
- **THEN** el sistema muestra un calendario para elegir fecha de inicio y fin

#### Scenario: Recalcular métricas con el rango confirmado
- **WHEN** el usuario eligió una fecha de inicio y una de fin en el calendario y confirma el rango
- **THEN** el sistema recalcula las métricas (tarjetas, gráficas, ranking) limitadas a ese rango

#### Scenario: Rango inválido (fin anterior a inicio)
- **WHEN** el usuario selecciona una fecha de fin anterior a la fecha de inicio e intenta confirmar el rango
- **THEN** el sistema impide confirmar y muestra un mensaje indicando que el rango es inválido

### Requirement: Persistir el filtro de periodo de Inicio entre sesiones
El sistema SHALL guardar en DataStore Preferences el último filtro de periodo seleccionado en Inicio (predefinido o rango personalizado) y SHALL restaurarlo automáticamente al reabrir la app.

#### Scenario: Restaurar el filtro de periodo al reabrir la app
- **WHEN** el usuario seleccionó el filtro "Semana" en Inicio, cierra la app y la reabre más tarde
- **THEN** el sistema muestra Inicio con el filtro "Semana" ya aplicado

#### Scenario: Restaurar un rango personalizado persistido
- **WHEN** el último filtro seleccionado fue un rango personalizado con fechas específicas y el usuario reabre la app
- **THEN** el sistema restaura ese rango exacto (fechas de inicio y fin), no solo la opción "Personalizado" vacía

#### Scenario: Primera vez sin filtro previo
- **WHEN** el usuario nunca seleccionó ningún filtro de periodo en Inicio y abre la app por primera vez
- **THEN** el sistema aplica un periodo por defecto razonable, sin error ni pantalla sin selección
