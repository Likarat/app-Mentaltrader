## ADDED Requirements

### Requirement: Ranking de emociones más frecuentes
El sistema SHALL mostrar un ranking de las emociones más frecuentes dentro del periodo seleccionado, ordenado de mayor a menor frecuencia.

#### Scenario: Ver el ranking de emociones con datos
- **WHEN** existen operaciones registradas con emociones asignadas dentro del periodo seleccionado y el usuario abre Inicio
- **THEN** el sistema muestra un ranking de las emociones más frecuentes en ese periodo, ordenado de mayor a menor frecuencia

### Requirement: Ranking de errores más frecuentes
El sistema SHALL mostrar un ranking de los errores más frecuentes dentro del periodo seleccionado, ordenado de mayor a menor frecuencia.

#### Scenario: Ver el ranking de errores con datos
- **WHEN** existen operaciones registradas con errores asignados dentro del periodo seleccionado y el usuario abre Inicio
- **THEN** el sistema muestra un ranking de los errores más frecuentes en ese periodo, ordenado de mayor a menor frecuencia

### Requirement: Desempate determinístico en el ranking
El sistema SHALL ordenar de forma determinística (alfabéticamente) los elementos del ranking que tengan exactamente la misma frecuencia, sin variar el orden entre renders.

#### Scenario: Empate en frecuencia
- **WHEN** dos o más elementos del ranking (de emociones o de errores) tienen exactamente la misma frecuencia en el periodo y el ranking se renderiza
- **THEN** el sistema los ordena alfabéticamente como criterio de desempate, sin variar el orden entre renders

### Requirement: Ranking vacío sin operaciones en el periodo
El sistema SHALL mostrar el ranking (de emociones y de errores) vacío, sin error, cuando no existan operaciones dentro del periodo seleccionado.

#### Scenario: Periodo sin operaciones
- **WHEN** no existen operaciones dentro del periodo seleccionado y el ranking se calcula
- **THEN** el sistema muestra el ranking vacío, sin errores
