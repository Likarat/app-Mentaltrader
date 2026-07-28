---
id: HU-029
titulo: Filtrar métricas por rango personalizado
epica: EP-004
prioridad: Could
complejidad: S
estado: lista
---

# Filtrar métricas por rango personalizado

## Historia

Como **trader de forex**,
quiero **seleccionar un rango de fechas personalizado en Inicio**,
para **analizar mis métricas en un periodo específico que no está cubierto por las opciones predefinidas**.

## Contexto

Se apoya en HU-028 (el selector de periodo ya debe existir); esta historia agrega la opción "Personalizado" dentro de ese mismo selector.

## Criterios de aceptación

### Escenario 1 — Happy path: elegir un rango personalizado
- **Dado que** el usuario está en Inicio
- **Cuando** selecciona "Personalizado"
- **Entonces** el sistema muestra un calendario para elegir fecha de inicio y fin

### Escenario 2 — Happy path: recalcular métricas con el rango confirmado
- **Dado que** el usuario eligió una fecha de inicio y una de fin en el calendario
- **Cuando** confirma el rango
- **Entonces** el sistema recalcula las métricas (tarjetas, gráficas, ranking) limitadas a ese rango

### Escenario 3 — Edge: rango inválido (fin anterior a inicio)
- **Dado que** el usuario selecciona una fecha de fin anterior a la fecha de inicio
- **Cuando** intenta confirmar el rango
- **Entonces** el sistema impide confirmar y muestra un mensaje indicando que el rango es inválido

## Notas técnicas (opcional)

- Recalcula sobre el mismo mecanismo de HU-026/HU-027, con el rango de fechas del calendario en vez de un periodo predefinido.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-028 (mismo epic, el selector ya debe existir), sin acoplarse a otra épica.
- [x] **N**egotiable — el componente exacto de calendario es negociable.
- [x] **V**aluable — sí: permite analizar cualquier ventana de tiempo específica, no solo las predefinidas.
- [x] **E**stimable — alcance acotado a un selector de rango + validación de coherencia de fechas.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (calendario mostrado, métricas recalculadas, rango inválido bloqueado).
