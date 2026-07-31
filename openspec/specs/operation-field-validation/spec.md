# operation-field-validation Specification

## Purpose
TBD - created by archiving change registro-rapido-operaciones. Update Purpose after archive.
## Requirements
### Requirement: Validar rangos numéricos y de fecha/hora al guardar
El sistema SHALL validar que Fecha/Hora no sea futura, Calidad esté entre 0.0 y 10.0, Riesgo (%) entre 0 y 100, y SHALL aceptar Resultado en R como cualquier decimal con signo sin límite, mostrándolo siempre con sufijo "R".

#### Scenario: Guardar con todos los valores numéricos dentro de rango
- **WHEN** el usuario completa Calidad, Riesgo (%) y Resultado en R dentro de sus rangos válidos, con una Fecha/Hora no futura, y guarda
- **THEN** el sistema almacena la operación sin mostrar ningún error de validación

#### Scenario: Fecha/hora futura
- **WHEN** el usuario ingresa una Fecha/Hora posterior al momento actual e intenta guardar
- **THEN** el sistema muestra el error "No se permiten fechas/horas futuras" en rojo e impide el guardado

#### Scenario: Calidad o Riesgo (%) fuera de rango
- **WHEN** el usuario ingresa un valor de Calidad fuera de 0.0–10.0 o de Riesgo (%) fuera de 0–100 e intenta guardar
- **THEN** el sistema impide el guardado y resalta el campo fuera de rango con un mensaje específico

#### Scenario: Resultado en R con magnitud grande y signo negativo
- **WHEN** el usuario ingresa un Resultado en R negativo de magnitud considerable y guarda
- **THEN** el sistema acepta el valor sin bloquear el guardado y lo muestra con el sufijo "R" fijo

