---
id: HU-012
titulo: Confirmar eliminación de un elemento de catálogo en uso
epica: EP-002
prioridad: Could
complejidad: S
estado: lista
---

# Confirmar eliminación de un elemento de catálogo en uso

## Historia

Como **trader de forex**,
quiero **recibir una advertencia con el conteo de operaciones afectadas al intentar eliminar un elemento de catálogo que ya está en uso**,
para **decidir con información completa si realmente quiero eliminarlo, sabiendo que el historial ya registrado se conserva**.

## Contexto

Se apoya en HU-011 (eliminar un elemento sin uso) — esta historia cubre el caso complementario: eliminar un elemento que sí está en uso. Requiere que existan operaciones registradas usando el elemento para poder probarse de punta a punta (dependencia natural con EP-001 para tener datos de prueba, no una duplicación de alcance). Al confirmarse la eliminación, el elemento deja de ofrecerse en nuevos formularios pero las operaciones ya registradas conservan su valor histórico (no se reescribe ni se pone en null retroactivamente).

## Criterios de aceptación

### Escenario 1 — Happy path: advertencia al eliminar un elemento en uso
- **Dado que** un elemento de catálogo ya fue usado en una o más operaciones existentes
- **Cuando** el usuario intenta eliminarlo desde la pestaña de Etiquetas
- **Entonces** el sistema muestra una advertencia indicando en cuántas operaciones está en uso, permitiendo confirmar o cancelar la eliminación

### Escenario 2 — Edge: cancelar la eliminación
- **Dado que** la advertencia de eliminación está visible para un elemento en uso
- **Cuando** el usuario elige cancelar
- **Entonces** el sistema cierra la advertencia y el elemento permanece sin cambios en el catálogo

### Escenario 3 — Edge: confirmar la eliminación conservando el valor histórico
- **Dado que** la advertencia de eliminación está visible para un elemento en uso
- **Cuando** el usuario confirma la eliminación
- **Entonces** el sistema retira el elemento del catálogo (deja de ofrecerse en nuevos formularios) y las operaciones que ya lo usaban conservan su valor histórico sin cambios

## Notas técnicas (opcional)

- El conteo de operaciones afectadas requiere una consulta de uso por `CatalogItem.id` antes de mostrar la advertencia.
- Consistente con el Requirement "Gestión de catálogos" de la spec: "conservar el valor histórico en las operaciones ya registradas" al eliminar un elemento en uso.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-011 (mismo epic, caso complementario) y, para pruebas de punta a punta, de que existan operaciones (EP-001) — dependencia de datos de prueba, no de alcance.
- [x] **N**egotiable — el texto exacto de la advertencia y el mecanismo de conteo quedan abiertos al equipo.
- [x] **V**aluable — sí: evita eliminar accidentalmente un elemento que el usuario todavía usa activamente en su historial.
- [x] **E**stimable — alcance acotado a una consulta de conteo + diálogo de confirmación.
- [x] **S**mall — un solo flujo (contar, advertir, confirmar/cancelar), cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (advertencia con conteo, elemento sin cambios, valor histórico conservado).
