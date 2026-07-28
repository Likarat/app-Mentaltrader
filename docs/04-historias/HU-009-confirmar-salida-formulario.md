---
id: HU-009
titulo: Confirmar antes de salir del formulario con cambios sin guardar
epica: EP-001
prioridad: Should
complejidad: S
estado: lista
---

# Confirmar antes de salir del formulario con cambios sin guardar

## Historia

Como **trader de forex**,
quiero **que el sistema me pida confirmación antes de salir del formulario si modifiqué algún campo sin guardar**,
para **evitar perder accidentalmente el registro de una operación en la que ya invertí tiempo completando datos**.

## Contexto

Última historia prevista de EP-001; con ella la épica de registro rápido queda completa. Se apoya en HU-001 (el formulario ya debe existir); no depende de ninguna otra épica. El diálogo debe dispararse tanto al navegar a otra pestaña como al presionar "atrás", y solo cuando hay al menos un campo modificado (si el usuario no tocó nada, debe poder salir sin fricción).

## Criterios de aceptación

### Escenario 1 — Happy path: mostrar diálogo al navegar a otra pestaña con cambios pendientes
- **Dado que** el usuario modificó al menos un campo del formulario sin guardar
- **Cuando** intenta navegar a otra pestaña
- **Entonces** el sistema muestra un diálogo de confirmación indicando que los cambios se perderán

### Escenario 2 — Edge: mostrar diálogo al presionar "atrás" con cambios pendientes
- **Dado que** el usuario modificó al menos un campo del formulario sin guardar
- **Cuando** presiona el botón/gesto "atrás" del sistema
- **Entonces** el sistema muestra el mismo diálogo de confirmación indicando que los cambios se perderán

### Escenario 3 — Edge: cancelar la salida y permanecer en el formulario
- **Dado que** el diálogo de confirmación está visible
- **Cuando** el usuario elige "Cancelar" (quedarse)
- **Entonces** el sistema cierra el diálogo y permanece en el formulario con todos los datos ingresados intactos

### Escenario 4 — Edge: confirmar la salida y perder los cambios
- **Dado que** el diálogo de confirmación está visible
- **Cuando** el usuario elige confirmar la salida
- **Entonces** el sistema abandona el formulario sin guardar, descartando los cambios ingresados

### Escenario 5 — Edge: salir sin fricción cuando no hay cambios pendientes
- **Dado que** el usuario no modificó ningún campo del formulario
- **Cuando** navega a otra pestaña o presiona "atrás"
- **Entonces** el sistema sale del formulario de inmediato, sin mostrar ningún diálogo de confirmación

## Notas técnicas (opcional)

- El diálogo debe dispararse por ambos gestos de salida: navegación a otra pestaña y botón/gesto "atrás" del sistema.
- Sin cambios pendientes, la salida debe ser inmediata, sin mostrar ningún diálogo.

## Checklist INVEST

- [x] **I**ndependent — sí: solo depende secuencialmente de HU-001 (mismo epic, el formulario ya debe existir), sin acoplarse a ninguna otra épica.
- [x] **N**egotiable — el "qué" (confirmar antes de perder cambios) está fijado por la spec; el "cómo" exacto (texto del diálogo, estilo de botones) queda abierto al equipo.
- [x] **V**aluable — sí: evita la pérdida accidental de datos ya ingresados, protegiendo el esfuerzo de registro del usuario.
- [x] **E**stimable — alcance acotado a un diálogo de confirmación con condición de "dirty state", sin ambigüedad.
- [x] **S**mall — un diálogo de confirmación sobre navegación/back, sin lógica de negocio nueva; cabe claramente en menos de 2-4 días.
- [x] **T**estable — 5 escenarios en Given/When/Then con resultados observables (diálogo mostrado/no mostrado, datos conservados o descartados según la elección del usuario).
