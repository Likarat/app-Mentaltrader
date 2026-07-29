---
id: HU-005
titulo: Prellenar fecha/hora y activo por defecto al abrir "Nueva operación"
epica: EP-001
prioridad: Should
complejidad: S
estado: lista
---

# Prellenar fecha/hora y activo por defecto al abrir "Nueva operación"

**Construcción**: `openspec_change: registro-rapido-operaciones` (rama `feature/ep-001-registro-rapido-operaciones`).

## Historia

Como **trader de forex**,
quiero **que el formulario de "Nueva operación" prellene automáticamente la Fecha y Hora actuales (en la misma fila) y el Activo con "XAUUSD" si el catálogo de Activos está vacío**,
para **registrar una operación más rápido, sin tener que completar manualmente los datos que casi siempre son los mismos**.

## Contexto

Se apoya en los campos y controles ya definidos por HU-001 (Fecha, Hora, Activo). Esta historia no agrega campos nuevos: agrega los valores por defecto que sostienen directamente el objetivo #1 del PRD (registro en ≤30 segundos de interacción). Los valores prellenados deben seguir siendo editables manualmente por el usuario (no son de solo lectura).

**Nota post-`/trycore:invest`**: el Escenario 5 original reutilizaba la mecánica de bloqueo de HU-004 (fecha/hora futura), lo cual sería una dependencia de comportamiento, no solo de estructura. El agente `invest-validator` confirmó que esto no rompe **Independent** (el valor central de esta historia no depende de HU-004), pero señaló que ese escenario duplicaba alcance ya cubierto por HU-004. Se ajustó el Escenario 5 para que describa únicamente el comportamiento de *prellenado* ante un reloj desconfigurado, dejando la validación de fecha/hora futura como responsabilidad exclusiva de HU-004.

## Criterios de aceptación

### Escenario 1 — Happy path: prellenado de Fecha y Hora al abrir el formulario
- **Dado que** el usuario abre "Nueva operación"
- **Cuando** la pantalla termina de cargar
- **Entonces** el sistema muestra Fecha y Hora prellenadas con el valor actual del dispositivo, ambas en la misma fila

### Escenario 2 — Happy path: preselección de Activo cuando el catálogo está vacío
- **Dado que** el catálogo de Activos no tiene ningún elemento agregado
- **Cuando** el usuario abre "Nueva operación"
- **Entonces** el sistema preselecciona "XAUUSD" como Activo

### Escenario 3 — Edge: catálogo de Activos con elementos ya agregados
- **Dado que** el catálogo de Activos ya tiene al menos un elemento agregado por el usuario
- **Cuando** el usuario abre "Nueva operación"
- **Entonces** el sistema no fuerza ningún valor por defecto de Activo, dejando el campo pendiente de selección manual

### Escenario 4 — Edge: edición manual de los valores prellenados
- **Dado que** el formulario ya muestra Fecha y Hora prellenadas
- **Cuando** el usuario modifica manualmente cualquiera de los dos valores antes de guardar
- **Entonces** el sistema conserva el valor editado por el usuario y no lo revierte automáticamente al valor por defecto

### Escenario 5 — Edge: reloj del dispositivo desconfigurado
- **Dado que** el reloj del dispositivo está desconfigurado
- **Cuando** el usuario abre "Nueva operación"
- **Entonces** el sistema prellena Fecha y Hora igualmente con el valor (incorrecto) del dispositivo, sin intentar corregirlo ni validarlo en este punto — la validación de fecha/hora futura al guardar es responsabilidad exclusiva de HU-004

## Notas técnicas (opcional)

- Fecha y Hora van en la misma fila del formulario (decisión ya tomada, ver PRD/spec §1).
- El prellenado de Activo con "XAUUSD" solo aplica si el catálogo de Activos está vacío (spec §3.1); si ya existen elementos, no se debe forzar ningún valor por defecto específico.

## Checklist INVEST

- [x] **I**ndependent — sí: el valor central (prellenado) no requiere que HU-004 exista; solo depende secuencialmente de HU-001 (mismo epic). Confirmado por `invest-validator` tras ajustar el Escenario 5 para no duplicar la mecánica de bloqueo de HU-004.
- [x] **N**egotiable — el "qué" (prellenar valores editables) está claro; el "cómo" (controles UI concretos) queda abierto al equipo.
- [x] **V**aluable — sí: reduce directamente la fricción de registro, sosteniendo el KPI de ≤30s del PRD.
- [x] **E**stimable — alcance acotado a valores por defecto sobre controles ya existentes, sin ambigüedad de mecanismo.
- [x] **S**mall — sin campos ni entidades nuevas; solo valores por defecto sobre controles existentes, cabe claramente en 2-4 días.
- [x] **T**estable — 5 escenarios en Given/When/Then con resultados observables (valor prellenado en pantalla, preselección condicional, persistencia de edición manual, comportamiento ante reloj desconfigurado).
