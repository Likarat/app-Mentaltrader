## ADDED Requirements

### Requirement: Tarjetas de resumen numérico
El sistema SHALL mostrar en Inicio tarjetas con: total de operaciones, % de operaciones ganadas/perdidas/break even, calidad promedio, R total acumulado y R promedio por operación, todas calculadas sobre el periodo seleccionado.

#### Scenario: Ver el resumen numérico con operaciones registradas
- **WHEN** existen operaciones registradas dentro del periodo seleccionado y el usuario abre Inicio
- **THEN** el sistema muestra tarjetas con total de operaciones, % ganadas/perdidas/break even, calidad promedio, R total acumulado y R promedio por operación

#### Scenario: Cálculo seguro con cero operaciones en el periodo
- **WHEN** no existen operaciones dentro del periodo seleccionado y el sistema calcula los porcentajes y promedios
- **THEN** el sistema evita divisiones por cero, mostrando valores neutros (0) sin errores ni caídas

### Requirement: Gráfica de línea de R acumulado
El sistema SHALL mostrar una gráfica de línea del R acumulado a lo largo del periodo seleccionado, construida punto a punto en orden cronológico.

#### Scenario: Ver la gráfica de línea con operaciones registradas
- **WHEN** existen operaciones registradas dentro del periodo seleccionado y la pantalla de Inicio se renderiza
- **THEN** el sistema muestra una gráfica de línea del R acumulado a lo largo del periodo

### Requirement: Gráfica de barras de distribución de resultados
El sistema SHALL mostrar una gráfica de barras con el % de operaciones Ganadas, Perdidas y Break Even del periodo seleccionado.

#### Scenario: Ver la gráfica de barras con operaciones registradas
- **WHEN** existen operaciones registradas dentro del periodo seleccionado y la pantalla de Inicio se renderiza
- **THEN** el sistema muestra una gráfica de barras con el % de operaciones Ganadas, Perdidas y Break Even

### Requirement: Codificación por color de valores positivos/negativos
El sistema SHALL mostrar en verde los valores agregados con signo positivo (ej. R total, R promedio) y en rojo los negativos, consistente con el resto de la app.

#### Scenario: Color de una métrica agregada con signo
- **WHEN** una métrica agregada con signo (ej. R total acumulado) se muestra en Inicio
- **THEN** el sistema la muestra en verde si es positiva y en rojo si es negativa
