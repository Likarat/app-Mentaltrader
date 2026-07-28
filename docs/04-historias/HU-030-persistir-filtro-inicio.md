---
id: HU-030
titulo: Persistir el filtro de periodo de Inicio entre sesiones
epica: EP-004
prioridad: Could
complejidad: S
estado: lista
---

# Persistir el filtro de periodo de Inicio entre sesiones

## Historia

Como **trader de forex**,
quiero **que el último filtro de periodo que seleccioné en Inicio se recuerde y se aplique automáticamente al reabrir la app**,
para **no tener que reseleccionarlo cada vez que quiero ver mis métricas**.

## Contexto

Se apoya en HU-028 y HU-029 (el selector de periodo y el rango personalizado ya deben existir). Mismo patrón que HU-024 (persistencia de filtro de Historial), pero aplicado al selector de periodo de Inicio, con DataStore Preferences.

## Criterios de aceptación

### Escenario 1 — Happy path: restaurar el filtro de periodo al reabrir la app
- **Dado que** el usuario seleccionó el filtro "Semana" en Inicio
- **Cuando** cierra la app y la reabre más tarde
- **Entonces** el sistema muestra Inicio con el filtro "Semana" ya aplicado

### Escenario 2 — Edge: restaurar un rango personalizado persistido
- **Dado que** el último filtro seleccionado fue un rango personalizado con fechas específicas
- **Cuando** el usuario reabre la app
- **Entonces** el sistema restaura ese rango exacto (fechas de inicio y fin), no solo la opción "Personalizado" vacía

### Escenario 3 — Edge: primera vez sin filtro previo
- **Dado que** el usuario nunca seleccionó ningún filtro de periodo en Inicio
- **Cuando** abre la app por primera vez
- **Entonces** el sistema aplica un periodo por defecto razonable, sin error ni pantalla sin selección

## Notas técnicas (opcional)

- DataStore Preferences, mismo mecanismo que HU-024.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-028/HU-029 (mismo epic, el selector ya debe existir), sin acoplarse a otra épica.
- [x] **N**egotiable — el valor por defecto exacto (Mes, Semana, etc.) es negociable.
- [x] **V**aluable — sí: evita reseleccionar el periodo en cada apertura de la app.
- [x] **E**stimable — alcance acotado, mismo patrón ya resuelto en HU-024.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (filtro restaurado, rango personalizado restaurado, valor por defecto en primera vez).
