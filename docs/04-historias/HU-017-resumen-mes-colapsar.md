---
id: HU-017
titulo: Ver resumen rápido por mes y colapsar/expandir el grupo
epica: EP-003
prioridad: Should
complejidad: S
estado: lista
---

# Ver resumen rápido por mes y colapsar/expandir el grupo

## Historia

Como **trader de forex**,
quiero **ver un resumen rápido (número de operaciones, % ganadas, R acumulado) en el encabezado de cada mes, y poder colapsar o expandir ese grupo tocándolo**,
para **tener una vista rápida de mi desempeño mensual sin tener que abrir cada operación**.

## Contexto

Se apoya en HU-015 (la agrupación por mes ya debe existir). Agrupa dos comportamientos de la spec que viven en el mismo encabezado de grupo: el resumen agregado y el toggle de colapsar/expandir (el toggle "existe, pero no es protagonista visualmente" según la spec).

## Criterios de aceptación

### Escenario 1 — Happy path: mostrar el resumen agregado del mes
- **Dado que** el usuario ve un grupo de mes en el Historial
- **Cuando** el grupo se renderiza
- **Entonces** el sistema muestra un resumen visible: número de operaciones, % de ganadas y R acumulado del grupo

### Escenario 2 — Happy path: colapsar y expandir un mes
- **Dado que** el usuario está viendo el listado agrupado
- **Cuando** toca el encabezado de un mes
- **Entonces** el sistema alterna entre expandir y colapsar ese grupo

### Escenario 3 — Edge: mes sin operaciones ganadas
- **Dado que** un mes tiene operaciones registradas pero ninguna con resultado "Ganada"
- **Cuando** se renderiza su resumen
- **Entonces** el sistema muestra 0% de ganadas, sin errores de cálculo

### Escenario 4 — Edge: R acumulado negativo coloreado
- **Dado que** el R acumulado del mes es negativo
- **Cuando** se muestra en el resumen
- **Entonces** el sistema lo muestra en rojo, consistente con la codificación de color del resto de la app

## Notas técnicas (opcional)

- Cálculo de % ganadas y R acumulado resuelto a nivel de base de datos (Room), no en memoria (spec §8/§9).
- El toggle de colapsar/expandir no debe ser visualmente protagonista (spec: "esta opción existe, pero no es protagonista visualmente").

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-015 (mismo epic, la agrupación ya debe existir), sin acoplarse a otra épica.
- [x] **N**egotiable — el diseño exacto del toggle y del resumen es negociable.
- [x] **V**aluable — sí: da visibilidad rápida de desempeño mensual sin abrir cada operación, contribuye al objetivo #3 (detección de patrones).
- [x] **E**stimable — alcance acotado a una agregación + un toggle de UI.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (resumen mostrado, toggle funcional, caso 0%, color según signo).
