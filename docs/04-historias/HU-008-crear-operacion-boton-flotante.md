---
id: HU-008
titulo: Crear operación desde el botón flotante "+"
epica: EP-001
prioridad: Should
complejidad: S
estado: lista
---

# Crear operación desde el botón flotante "+"

**Construcción**: `openspec_change: registro-rapido-operaciones` (rama `feature/ep-001-registro-rapido-operaciones`).

## Historia

Como **trader de forex**,
quiero **tocar un botón flotante "+" desde Inicio o Historial para abrir directamente el formulario de "Nueva operación"**,
para **registrar una operación de inmediato sin tener que navegar por menús**.

## Contexto

Se apoya en HU-001 (el formulario de "Nueva operación" ya debe existir) y en HU-032 (navegación de pestañas, EP-005) para que Inicio y Historial existan como destinos navegables — esta es una dependencia de secuenciación normal de infraestructura de navegación, no una duplicación de alcance (a diferencia del caso ya resuelto entre HU-006 y la historia de permisos que se retiró de EP-005 por redundante, ver `docs/03-backlog/epicas.md` §Reporte de cobertura). El botón flotante "+" **no** debe aparecer en la pestaña Etiquetas (el agregar ahí ocurre dentro de cada sección de catálogo, ver EP-002).

## Criterios de aceptación

### Escenario 1 — Happy path: crear operación desde el FAB en Inicio
- **Dado que** el usuario está en la pestaña Inicio
- **Cuando** toca el botón flotante "+"
- **Entonces** el sistema abre el formulario de "Nueva operación"

### Escenario 2 — Happy path: crear operación desde el FAB en Historial
- **Dado que** el usuario está en la pestaña Historial
- **Cuando** toca el botón flotante "+"
- **Entonces** el sistema abre el formulario de "Nueva operación"

### Escenario 3 — Edge: ausencia del FAB en la pestaña Etiquetas
- **Dado que** el usuario está en la pestaña Etiquetas
- **Cuando** visualiza la pantalla
- **Entonces** el sistema no muestra ningún botón flotante "+"

### Escenario 4 — Edge: el FAB no tapa contenido al hacer scroll
- **Dado que** el usuario está en Inicio o Historial con contenido suficiente para hacer scroll
- **Cuando** desplaza la pantalla
- **Entonces** el botón flotante permanece visible y no bloquea la interacción con el contenido subyacente

## Notas técnicas (opcional)

- El FAB no debe tapar contenido al hacer scroll (ver PRD/spec, Requirement "Pestaña de Inicio").
- Mismo formulario de "Nueva operación" que HU-001; esta historia solo agrega el punto de entrada, no lógica de formulario nueva.

## Checklist INVEST

- [x] **I**ndependent — sí (con nota): depende de HU-001 (mismo epic) y de HU-032/EP-005 (navegación de pestañas) como infraestructura de plataforma con alcance disjunto y no duplicado (a diferencia del caso HU-006/permisos). Es dependencia unidireccional de secuenciación, no de fusión de alcance — declarar el orden HU-032 → HU-008 al priorizar (`/trycore:priorizar` o `docs/03-backlog/backlog.md`).
- [x] **N**egotiable — el "qué" (entrada rápida al formulario) está claro; el "cómo" exacto (posición/animación del FAB) queda abierto.
- [x] **V**aluable — sí: reduce la fricción de acceso al registro, sosteniendo el KPI de ≤30s del PRD.
- [x] **E**stimable — alcance acotado (solo navegación/entry point, reutiliza el formulario de HU-001).
- [x] **S**mall — sin lógica de negocio nueva; solo un punto de entrada de navegación, cabe claramente en menos de 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (abre formulario desde Inicio/Historial, ausencia de FAB en Etiquetas, FAB no bloquea contenido en scroll).
