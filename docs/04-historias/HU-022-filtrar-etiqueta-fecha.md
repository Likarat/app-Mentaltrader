---
id: HU-022
titulo: Filtrar operaciones por etiqueta y por fecha/periodo (AND)
epica: EP-003
prioridad: Should
complejidad: M
estado: lista
---

# Filtrar operaciones por etiqueta y por fecha/periodo (AND)

## Historia

Como **trader de forex**,
quiero **filtrar mi historial por Activo, Resultado, Error, Emoción antes/después y por fecha o periodo, combinando varios filtros a la vez**,
para **encontrar rápido el subconjunto de operaciones que quiero analizar**.

## Contexto

Primer slice vertical de "filtros y búsqueda" (originalmente una única historia con 6 tipos de filtro de etiqueta + fecha + búsqueda de texto + combinación AND, dividida proactivamente por el mismo patrón de tamaño ya visto en HU-001/006/010/015: demasiadas piezas heterogéneas). Este slice cubre los filtros de etiqueta (Activo, Resultado, Error, Emoción antes, Emoción después) y de fecha/periodo (Última semana, Último mes, Este mes, Últimos 3 meses, Personalizado), combinados con lógica **AND**, más la acción de limpiar todos los filtros. **HU-023** añade sobre esta base la búsqueda por palabra clave.

Se apoya en HU-015 (el listado agrupado ya debe existir, los filtros se aplican sobre él).

## Criterios de aceptación

### Escenario 1 — Happy path: filtrar por una etiqueta
- **Dado que** el usuario está en Historial
- **Cuando** aplica un filtro por Activo, Emoción, Error o Resultado
- **Entonces** el sistema muestra solo las operaciones que coincidan, manteniendo el agrupamiento por mes

### Escenario 2 — Happy path: filtrar por fecha o periodo
- **Dado que** el usuario está en Historial
- **Cuando** selecciona una fecha específica, un periodo predefinido (ej. "Última semana") o un rango personalizado
- **Entonces** el sistema muestra solo las operaciones dentro de ese rango

### Escenario 3 — Edge: combinar múltiples filtros de etiqueta con lógica AND
- **Dado que** el usuario aplicó un filtro de Activo y un filtro de Resultado al mismo tiempo
- **Cuando** ambos filtros están activos
- **Entonces** el sistema muestra solo las operaciones que cumplen ambos criterios simultáneamente (AND), no solo uno de ellos

### Escenario 4 — Edge: limpiar todos los filtros
- **Dado que** el usuario tiene uno o más filtros activos
- **Cuando** toca "Limpiar filtros"
- **Entonces** el sistema remueve todos los filtros y muestra el listado completo agrupado por mes

## Notas técnicas (opcional)

- "Última semana" es un filtro (últimos 7 días), no afecta la agrupación visual, que sigue siendo solo por mes.
- La búsqueda por palabra clave y su combinación con estos filtros es responsabilidad de HU-023, no de esta historia.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-015 (mismo epic, el listado ya debe existir), sin acoplarse a otra épica; no depende de HU-023 (la búsqueda de texto es una extensión, no un prerequisito).
- [x] **N**egotiable — el diseño exacto de los controles de filtro (chips, dropdowns) es negociable.
- [x] **V**aluable — sí: permite encontrar rápido el subconjunto de operaciones relevante, una de las formas de detectar patrones (objetivo #3).
- [x] **E**stimable — alcance acotado a 6 tipos de filtro + combinación AND, con reglas ya fijadas por la spec.
- [x] **S**mall — alcance reducido respecto a la historia original (sin búsqueda de texto); cabe razonablemente en 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (filtrado por etiqueta, por fecha, combinación AND, limpieza de filtros).
