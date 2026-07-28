---
id: HU-004
titulo: Validar rangos numéricos y de fecha/hora al guardar
epica: EP-001
prioridad: Must
complejidad: S
estado: lista
---

# Validar rangos numéricos y de fecha/hora al guardar

## Historia

Como **trader de forex**,
quiero **que el sistema valide los rangos de Calidad, Riesgo (%), Resultado en R y que la Fecha/Hora no sea futura al guardar una operación**,
para **evitar registrar datos inconsistentes o erróneos que distorsionen mis métricas y el estudio de patrones más adelante**.

## Contexto

Se apoya en los campos ya definidos por HU-001 (Fecha, Hora, Resultado, Riesgo %, Resultado en R) y HU-002 (Calidad). Esta historia no agrega campos nuevos ni UI nueva: agrega la capa de validación de rango sobre campos que ya existen. Reglas exactas (spec, Requirement "Reglas de validación"):

| Campo | Regla |
|---|---|
| Fecha y Hora | No se permiten fechas ni horas futuras. Mensaje: "No se permiten fechas/horas futuras" |
| Calidad | Solo entre 0.0 y 10.0, máximo 1 decimal |
| Riesgo (%) | Solo entre 0 y 100 |
| Resultado en R | Cualquier valor decimal con signo, sin límite superior o inferior; se muestra siempre con sufijo "R" |

## Criterios de aceptación

### Escenario 1 — Happy path: guardar con todos los valores numéricos dentro de rango
- **Dado que** el usuario completó Calidad (ej. 7.5), Riesgo % (ej. 2), Resultado en R (ej. +2) y una Fecha/Hora no futura
- **Cuando** presiona "Guardar"
- **Entonces** el sistema almacena la operación con normalidad, sin mostrar ningún error de validación

### Escenario 2 — Error: fecha/hora futura
- **Dado que** el usuario ingresó una Fecha/Hora posterior al momento actual
- **Cuando** intenta guardar
- **Entonces** el sistema muestra el error "No se permiten fechas/horas futuras" en rojo e impide el guardado

### Escenario 3 — Error: Calidad o Riesgo (%) fuera de rango
- **Dado que** el usuario ingresó un valor de Calidad fuera de 0.0–10.0 (ej. 11) o de Riesgo (%) fuera de 0–100 (ej. 150)
- **Cuando** intenta guardar
- **Entonces** el sistema impide el guardado y resalta el campo fuera de rango con un mensaje de error específico, sin bloquear los demás campos ya válidos

### Escenario 4 — Edge: Resultado en R con magnitud grande y signo negativo
- **Dado que** el usuario ingresó un Resultado en R negativo de magnitud considerable (ej. -15.5), sin que exista límite inferior definido para este campo
- **Cuando** guarda la operación
- **Entonces** el sistema acepta el valor sin bloquear el guardado y lo muestra en toda la UI con el sufijo "R" fijo (ej. "-15.5R")

## Notas técnicas (opcional)

- Validación de "fecha/hora no futura" debe compararse contra la hora actual del dispositivo en el momento de guardar, no en el momento de abrir el formulario (evita falsos negativos si el formulario queda abierto un rato).
- Resultado en R: el signo se define exclusivamente por el dropdown "+/-" del formulario (ver PRD/spec, Requirement "Formulario de registro"), nunca escrito como texto libre; el teclado numérico del campo se restringe a dígitos y separador decimal.

## Checklist INVEST

- [x] **I**ndependent — sí: solo depende secuencialmente de HU-001/HU-002 dentro de la misma épica (los campos deben existir antes de validarse), sin acoplarse a EP-002 ni a ninguna otra épica — mismo criterio ya aplicado a HU-002/HU-003.
- [x] **N**egotiable — los umbrales exactos (0.0–10.0, 0–100) están fijados por la spec y no son negociables, pero el mecanismo de presentación del error (inline, color, snackbar) sí lo es — suficiente margen de "cómo" para el equipo.
- [x] **V**aluable — sí: protege la integridad de los datos que sostienen las métricas de EP-004 (R acumulado, ranking) y el propio propósito del producto (detectar patrones reales, no ruido de datos mal cargados).
- [x] **E**stimable — 4 reglas de validación puntuales y bien definidas, sin ambigüedad de alcance ni dependencias técnicas ocultas.
- [x] **S**mall — sin UI nueva ni entidades nuevas; solo lógica de validación sobre 4 reglas puntuales de campos existentes, cabe razonablemente en 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (mensaje de error específico, bloqueo o aceptación del guardado, formato de visualización con sufijo "R").
