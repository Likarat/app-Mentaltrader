---
id: HU-023
titulo: Buscar por palabra clave, combinada con los filtros de etiqueta
epica: EP-003
prioridad: Could
complejidad: S
estado: lista
---

# Buscar por palabra clave, combinada con los filtros de etiqueta

## Historia

Como **trader de forex**,
quiero **buscar operaciones por palabra clave en mis descripciones y motivos, combinando la búsqueda con los filtros de etiqueta ya aplicados**,
para **encontrar una operación específica aunque no recuerde exactamente qué etiquetas le puse**.

## Contexto

Segundo slice vertical de "filtros y búsqueda" (ver HU-022 para el primero). Depende de HU-022 para la lógica de combinación AND ya existente — esta historia extiende esa combinación agregando la búsqueda de texto como un criterio más.

## Criterios de aceptación

### Escenario 1 — Happy path: buscar por palabra clave
- **Dado que** el usuario está en Historial
- **Cuando** ingresa una palabra clave en el buscador
- **Entonces** el sistema muestra las operaciones cuyos campos de texto (`entryDescription`, `emotionBeforeReason`, `emotionAfterReason`, `errorReason`) o nombres de catálogo seleccionados contengan esa palabra

### Escenario 2 — Edge: combinar búsqueda de texto con un filtro de etiqueta ya activo
- **Dado que** el usuario aplicó el filtro Activo = "XAUUSD"
- **Cuando** adicionalmente escribe "ruptura" en el buscador
- **Entonces** el sistema muestra solo operaciones de XAUUSD cuyos campos de texto contengan "ruptura" (AND con el filtro de HU-022)

### Escenario 3 — Edge: búsqueda sin coincidencias
- **Dado que** el usuario ingresa una palabra clave que no coincide con ningún campo de texto ni catálogo de ninguna operación
- **Cuando** la búsqueda se ejecuta
- **Entonces** el sistema muestra el listado vacío para ese criterio, sin error

## Notas técnicas (opcional)

- Búsqueda parcial (coincidencia de subcadena), no exacta.
- Reutiliza la lógica de combinación AND ya construida en HU-022, agregando el criterio de texto.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-022 (mismo epic, reutiliza su lógica de combinación), sin acoplarse a otra épica.
- [x] **N**egotiable — el motor de búsqueda exacto (`LIKE` vs `FTS` en SQLite) es negociable, ver spec §11 "Próximos pasos sugeridos".
- [x] **V**aluable — sí: permite encontrar una operación específica sin recordar exactamente sus etiquetas.
- [x] **E**stimable — alcance acotado a una consulta de texto sobre 4 campos + nombres de catálogo.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (resultados de búsqueda, combinación AND, listado vacío sin error).
