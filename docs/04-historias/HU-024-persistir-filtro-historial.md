---
id: HU-024
titulo: Persistir el último filtro de Historial entre sesiones
epica: EP-003
prioridad: Could
complejidad: S
estado: lista
---

# Persistir el último filtro de Historial entre sesiones

## Historia

Como **trader de forex**,
quiero **que el último filtro que apliqué en Historial se recuerde y se restaure automáticamente al reabrir la app**,
para **no tener que reconfigurar mis filtros cada vez que quiero revisar el mismo subconjunto de operaciones**.

## Contexto

Se apoya en HU-022 y HU-023 (los filtros y la búsqueda ya deben existir para poder persistir su estado). Usa DataStore Preferences con las claves ya definidas en la spec: `lastPeriodFilter`, `lastCustomRangeStart`/`lastCustomRangeEnd`, `lastSearchText`, `lastSelectedAssets`, `lastSelectedResults`, `lastSelectedErrors`, `lastSelectedEmotionBefore`, `lastSelectedEmotionAfter`.

## Criterios de aceptación

### Escenario 1 — Happy path: restaurar filtros al reabrir Historial
- **Dado que** el usuario aplicó filtros en Historial
- **Cuando** cierra la app y la reabre
- **Entonces** el sistema restaura el último estado de filtros guardado

### Escenario 2 — Edge: persistencia real entre reinicios completos de la app
- **Dado que** el usuario aplicó filtros y cerró completamente la app (proceso terminado, no solo en segundo plano)
- **Cuando** la vuelve a abrir
- **Entonces** los filtros siguen restaurados, confirmando que la persistencia es en disco (DataStore) y no solo en memoria de sesión

### Escenario 3 — Edge: primera vez sin filtros previos
- **Dado que** el usuario nunca aplicó ningún filtro en Historial
- **Cuando** abre Historial por primera vez
- **Entonces** el sistema lo muestra sin ningún filtro activo, con el comportamiento por defecto

## Notas técnicas (opcional)

- DataStore Preferences, claves ya definidas en la spec (ver Contexto).

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-022/HU-023 (mismo epic, los filtros ya deben existir), sin acoplarse a otra épica.
- [x] **N**egotiable — el mecanismo de persistencia (DataStore) ya está fijado por decisión de stack del proyecto, no por esta historia.
- [x] **V**aluable — sí: evita reconfigurar filtros repetidamente, ahorra tiempo de uso diario.
- [x] **E**stimable — alcance acotado a persistir/restaurar un conjunto de claves ya definidas.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (filtros restaurados, persistencia real, estado por defecto).
