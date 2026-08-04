---
id: HU-004
titulo: Validar rangos numéricos y de fecha/hora al guardar
epica: EP-001
prioridad: Must
complejidad: S
estado: lista
---

# Validar rangos numéricos y de fecha/hora al guardar

**Construcción**: `openspec_change: registro-rapido-operaciones` (rama `feature/ep-001-registro-rapido-operaciones`).

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
| Resultado en R | Decimal con signo, sin límite superior o inferior en magnitud; se muestra siempre con sufijo "R". El **signo** sí está acotado por el campo Resultado (ver Escenario 5, enmienda 2026-08-04): Ganada exige R ≥ 0, Perdida exige R ≤ 0, Break Even acepta cualquier signo (incluido 0) |

**Enmienda 2026-08-04** (feedback real de uso, post-archivo de EP-001): la regla de signo de "Resultado en R" arriba y el Escenario 5 se agregaron después de que el usuario detectara, probando la app ya construida, que podía guardar una operación "Ganada" con R negativo (o "Perdida" con R positivo) sin ningún aviso — una incoherencia real de datos que distorsiona exactamente las métricas que esta historia existe para proteger (ver Historia arriba). Se decidió, explícitamente y por acuerdo con el usuario, tratar esto como una enmienda de esta historia ya archivada (agregar el escenario + implementar la validación) en vez de reabrir formalmente la épica EP-001, dado que es una única regla de validación acotada sobre un campo ya existente, sin UI ni modelo de datos nuevo. Queda registrado aquí para que la próxima auditoría de coherencia (`coherence-three-way`) encuentre el AC y el escenario ya alineados con el código.

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
- **Dado que** el usuario seleccionó Resultado = Perdida e ingresó un Resultado en R negativo de magnitud considerable (ej. -15.5), sin que exista límite inferior definido para la magnitud de este campo
- **Cuando** guarda la operación
- **Entonces** el sistema acepta el valor sin bloquear el guardado y lo muestra en toda la UI con el sufijo "R" fijo (ej. "-15.5R")

### Escenario 5 — Error: signo de Resultado en R incoherente con el Resultado (enmienda 2026-08-04)
- **Dado que** el usuario seleccionó Resultado = Ganada y un Resultado en R negativo (ej. -1.5), o Resultado = Perdida y un Resultado en R positivo (ej. +1.5)
- **Cuando** intenta guardar
- **Entonces** el sistema impide el guardado y muestra un mensaje de error específico en el campo Resultado en R indicando que el signo no es coherente con el Resultado seleccionado; con Resultado = Break Even, cualquier signo (incluido 0) se acepta sin error

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
