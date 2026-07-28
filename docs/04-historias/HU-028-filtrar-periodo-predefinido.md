---
id: HU-028
titulo: Filtrar métricas por periodo predefinido
epica: EP-004
prioridad: Should
complejidad: S
estado: lista
---

# Filtrar métricas por periodo predefinido

## Historia

Como **trader de forex**,
quiero **seleccionar un periodo predefinido (Día, Semana, Mes, Últimos 3 meses) en Inicio**,
para **recalcular mis métricas y ranking usando solo las operaciones de ese periodo**.

## Contexto

Provee el selector de periodo que HU-026 y HU-027 asumían ya existente. Es la pieza que conecta ambas historias con un rango de fechas real seleccionable por el usuario. El rango personalizado se cubre en **HU-029**, no aquí.

## Criterios de aceptación

### Escenario 1 — Happy path: recalcular métricas al seleccionar un periodo
- **Dado que** el usuario está en Inicio
- **Cuando** selecciona un periodo (Día, Semana, Mes o Últimos 3 meses)
- **Entonces** el sistema recalcula todas las métricas (tarjetas, gráficas, ranking) usando solo las operaciones dentro de ese periodo

### Escenario 2 — Edge: cambiar de un periodo a otro sucesivamente
- **Dado que** el usuario ya tiene un periodo seleccionado con métricas calculadas
- **Cuando** selecciona un periodo distinto
- **Entonces** el sistema recalcula todas las métricas desde cero para el nuevo periodo, sin arrastrar datos del periodo anterior

### Escenario 3 — Edge: selector fijo visible al hacer scroll
- **Dado que** el usuario hace scroll en la pantalla de Inicio
- **Cuando** el contenido se desplaza
- **Entonces** el selector de periodo permanece fijo y visible en la parte superior

## Notas técnicas (opcional)

- Recalcula las métricas de HU-026 y el ranking de HU-027 sobre el mismo rango de fechas seleccionado.

## Checklist INVEST

- [x] **I**ndependent — sí: complementa a HU-026/HU-027 (mismo epic) sin que ninguna dependa circularmente de la otra para existir; no depende de otra épica.
- [x] **N**egotiable — el diseño exacto del selector (chips, dropdown, tabs) es negociable.
- [x] **V**aluable — sí: permite enfocar el análisis en la ventana de tiempo relevante para el usuario.
- [x] **E**stimable — alcance acotado a 4 rangos predefinidos con recálculo ya conectado a HU-026/HU-027.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (recálculo, cambio limpio entre periodos, selector fijo).
