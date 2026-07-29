---
id: HU-003
titulo: Agregar descripción de entrada a la operación
epica: EP-001
prioridad: Must
complejidad: S
estado: lista
---

# Agregar descripción de entrada a la operación

**Construcción**: `openspec_change: registro-rapido-operaciones` (rama `feature/ep-001-registro-rapido-operaciones`).

## Historia

Como **trader de forex**,
quiero **agregar una descripción libre y multilínea de la entrada (Descripción entrada)**,
para **poder repasar después el razonamiento completo detrás de la operación al estudiarla**.

## Contexto

Tercer y último slice vertical de la creación de operaciones (ver HU-001 y HU-002). Cierra el conjunto completo de los 10 campos obligatorios del formulario. Es un campo de texto libre, multilínea, sin límite de longitud en UI ni modelo, y va al final del formulario (spec §4, Requirement "Registro de operación" y §1 "decisión ya tomada: Descripción entrada es el último campo").

## Criterios de aceptación

### Escenario 1 — Happy path: guardar una operación con Descripción entrada completa
- **Dado que** el usuario está completando el formulario y ya tiene el resto de campos obligatorios de HU-001/HU-002 llenos
- **Cuando** escribe texto en "Descripción entrada" y presiona "Guardar"
- **Entonces** el sistema almacena el texto completo en `entryDescription` junto con el resto de la operación

### Escenario 2 — Error: intento de guardar con "Descripción entrada" vacía
- **Dado que** el usuario dejó vacío el campo "Descripción entrada"
- **Cuando** presiona "Guardar"
- **Entonces** el sistema impide el guardado y resalta visualmente el campo "Descripción entrada" como obligatorio

### Escenario 3 — Edge: texto largo con múltiples saltos de línea
- **Dado que** el usuario escribe un texto extenso (varios párrafos, múltiples saltos de línea) en "Descripción entrada", sin límite de longitud aplicado
- **Cuando** el campo crece en pantalla para mostrar el contenido y el usuario presiona "Guardar"
- **Entonces** el sistema almacena el texto completo sin truncarlo, y el campo se muestra expandido según su contenido tanto en el formulario como en la vista de detalle

## Notas técnicas (opcional)

- Sin límite de longitud máxima obligatorio en UI ni modelo (spec §4); se recomienda un límite "razonable" por performance, pero no bloqueante.
- Campo multilínea expansible (crece con el contenido), último campo del formulario en el orden de layout definido.

## Checklist INVEST

- [x] **I**ndependent — sí: no depende de ningún catálogo ni de EP-002; solo depende de HU-001/HU-002 dentro de la misma épica (secuenciación normal).
- [x] **N**egotiable — el control de UI exacto (autosize, límite recomendado de UX) es negociable.
- [x] **V**aluable — sí: es el campo que sostiene el "estudio" cualitativo de la operación, propósito central del producto.
- [x] **E**stimable — alcance mínimo (un solo campo de texto), fácil de estimar.
- [x] **S**mall — claramente cabe en menos de 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (persistencia sin truncar, resaltado de campo obligatorio, expansión visual).
