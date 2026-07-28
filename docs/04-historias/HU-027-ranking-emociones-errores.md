---
id: HU-027
titulo: Ver ranking de emociones y errores más frecuentes por periodo
epica: EP-004
prioridad: Must
complejidad: S
estado: lista
---

# Ver ranking de emociones y errores más frecuentes por periodo

## Historia

Como **trader de forex**,
quiero **ver un ranking de las emociones y errores más frecuentes en el periodo seleccionado**,
para **identificar rápidamente qué patrones se repiten en mi operativa y corregirlos**.

## Contexto

Materializa de forma directa el propósito central del producto (detección de patrones, objetivo #3 del PRD). Depende de que existan operaciones (EP-001) con emociones/errores asignados desde los catálogos (EP-002). Asume un periodo ya seleccionado (mismo supuesto que HU-026).

## Criterios de aceptación

### Escenario 1 — Happy path: ranking de emociones más frecuentes
- **Dado que** existen operaciones registradas con emociones asignadas dentro del periodo seleccionado
- **Cuando** el usuario abre Inicio
- **Entonces** el sistema muestra un ranking de las emociones más frecuentes en ese periodo, ordenado de mayor a menor frecuencia

### Escenario 2 — Happy path: ranking de errores más frecuentes
- **Dado que** existen operaciones registradas con errores asignados dentro del periodo seleccionado
- **Cuando** el usuario abre Inicio
- **Entonces** el sistema muestra un ranking de los errores más frecuentes en ese periodo, ordenado de mayor a menor frecuencia

### Escenario 3 — Edge: empate en frecuencia
- **Dado que** dos o más elementos del ranking tienen exactamente la misma frecuencia en el periodo
- **Cuando** el ranking se renderiza
- **Entonces** el sistema los ordena de forma determinística (ej. alfabético como criterio de desempate), sin variar el orden entre renders

### Escenario 4 — Edge: periodo sin operaciones
- **Dado que** no existen operaciones dentro del periodo seleccionado
- **Cuando** el ranking se calcula
- **Entonces** el sistema muestra el ranking vacío, sin errores

## Notas técnicas (opcional)

- Cálculo resuelto a nivel de base de datos (Room), agrupando por `CatalogItem.id` y contando ocurrencias dentro del rango de fechas del periodo.
- El elemento semilla "Ninguno" (Error) puede aparecer en el ranking como cualquier otro valor si es el más usado — no se excluye especialmente.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de EP-001/EP-002 solo como datos de prueba (operaciones con catálogos asignados), no de alcance; asume un periodo ya seleccionado sin depender de HU-028/029/030.
- [x] **N**egotiable — el diseño visual del ranking (lista, barras) es negociable.
- [x] **V**aluable — sí: es el corazón del propósito del producto, detección de patrones sin esfuerzo manual.
- [x] **E**stimable — alcance acotado a dos consultas de agregación (conteo por catálogo) con criterio de orden fijado.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (ranking de emociones, ranking de errores, desempate determinístico, ranking vacío).
