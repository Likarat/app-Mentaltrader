---
id: HU-020
titulo: Editar una operación existente
epica: EP-003
prioridad: Must
complejidad: S
estado: lista
---

# Editar una operación existente

## Historia

Como **trader de forex**,
quiero **editar una operación existente desde su vista de detalle**,
para **corregir datos que ingresé incorrectamente sin tener que eliminar y volver a crear la operación**.

## Contexto

Se apoya en HU-018 (vista de detalle, botón "Editar") y reutiliza el mismo formulario y las mismas reglas de validación ya definidas en EP-001 (HU-001 a HU-004), ahora en modo edición (prellenado con los datos actuales en vez de valores por defecto). No redefine esas reglas, solo confirma que se apliquen también al editar.

## Criterios de aceptación

### Escenario 1 — Happy path: abrir el formulario prellenado con los datos actuales
- **Dado que** el usuario está en la vista de detalle de una operación
- **Cuando** presiona "Editar"
- **Entonces** el sistema abre el formulario prellenado con los datos actuales de esa operación

### Escenario 2 — Happy path: guardar los cambios editados
- **Dado que** el usuario modificó uno o más campos del formulario de edición
- **Cuando** presiona "Guardar"
- **Entonces** el sistema actualiza la operación con los nuevos valores, reflejándolos en el listado y en la vista de detalle

### Escenario 3 — Edge: las reglas de validación existentes aplican también al editar
- **Dado que** el usuario, editando una operación, ingresa un valor fuera de rango (misma regla de HU-004, ej. Calidad fuera de 0.0-10.0)
- **Cuando** intenta guardar
- **Entonces** el sistema aplica la misma validación que en creación y bloquea el guardado

## Notas técnicas (opcional)

- Reutiliza el mismo componente de formulario de HU-001/002/003, en modo edición (prellenado en vez de valores por defecto de HU-005).
- Las reglas de validación de rango son responsabilidad de HU-004; este escenario solo confirma que también se aplican en modo edición, no las revalida todas.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-018 (mismo epic) y reutiliza el formulario/validaciones de EP-001 como infraestructura ya existente, sin duplicar su alcance.
- [x] **N**egotiable — el diseño exacto de cómo se accede al modo edición es negociable.
- [x] **V**aluable — sí: permite corregir errores sin perder el resto del registro (eliminar y recrear sería más costoso).
- [x] **E**stimable — alcance acotado, reutiliza componentes ya construidos.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (formulario prellenado, cambios reflejados, validación aplicada).
