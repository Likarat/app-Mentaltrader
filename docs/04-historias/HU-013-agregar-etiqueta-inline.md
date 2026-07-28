---
id: HU-013
titulo: Agregar una nueva etiqueta sin salir del formulario de operación
epica: EP-002
prioridad: Should
complejidad: S
estado: lista
---

# Agregar una nueva etiqueta sin salir del formulario de operación

## Historia

Como **trader de forex**,
quiero **agregar una nueva etiqueta (Activo, Emoción o Error) directamente desde el formulario de operación, sin salir de él**,
para **no perder el contexto ni el progreso ya ingresado cuando la opción que necesito todavía no existe en el catálogo**.

## Contexto

Reutiliza la lógica de creación de elementos ya definida en **HU-010** (validación de unicidad, persistencia en `CatalogItem`), pero disparada desde un punto de entrada distinto — dentro del formulario de operación en vez de la pestaña Etiquetas — y con un comportamiento adicional propio: selección automática del nuevo elemento y permanencia en el formulario sin perder el resto de los datos ya ingresados. No redefine la regla de unicidad, solo la reutiliza.

## Criterios de aceptación

### Escenario 1 — Happy path: mostrar el campo inline al tocar "+ Agregar nueva"
- **Dado que** el usuario está seleccionando una etiqueta (ej. Activo) en el formulario de operación y no encuentra la opción deseada
- **Cuando** toca "+ Agregar nueva"
- **Entonces** el sistema muestra un campo pequeño para escribir el nombre del nuevo elemento, sin salir del formulario

### Escenario 2 — Happy path: guardar y seleccionar automáticamente sin salir del formulario
- **Dado que** el usuario escribió un nombre válido en el campo inline
- **Cuando** confirma la creación
- **Entonces** el sistema guarda el nuevo elemento en el catálogo correspondiente, lo selecciona automáticamente en el campo del formulario, y el resto de los datos ya ingresados en el formulario permanecen intactos

### Escenario 3 — Error: nombre duplicado desde el flujo inline
- **Dado que** el usuario intenta confirmar la creación de un elemento cuyo nombre ya existe en el catálogo correspondiente (misma regla de unicidad que valida HU-010, no se revalida aquí)
- **Cuando** el sistema rechaza la creación
- **Entonces** el campo inline permanece abierto y con foco para corregir el nombre, el mensaje "Este valor ya existe en el catálogo" se muestra junto al campo (sin interrumpir ni navegar fuera del formulario), y el resto de los datos ya ingresados en el formulario permanece intacto

### Escenario 4 — Edge: cancelar el campo inline sin crear ningún elemento
- **Dado que** el campo inline para agregar una nueva etiqueta está visible
- **Cuando** el usuario lo cierra sin escribir nada o cancela la acción
- **Entonces** el sistema cierra el campo y el selector de etiqueta vuelve a su estado anterior, sin crear ningún elemento nuevo

## Notas técnicas (opcional)

- Reutiliza la misma validación de unicidad de `CatalogItem` que HU-010 (case-insensitive, trim); no duplicar esa lógica, solo invocarla desde este punto de entrada.
- Aplica a los tres tipos de catálogo (`ASSET`, `EMOTION`, `ERROR`) según cuál selector disparó la acción.
- La lógica de detección de duplicados y el mensaje de error son responsabilidad de HU-010; el Escenario 3 de esta historia solo prueba el comportamiento del formulario/campo inline ante ese error (foco, no-navegación, datos intactos), no la regla de unicidad en sí.

## Checklist INVEST

- [x] **I**ndependent — sí: reutiliza la lógica de HU-010 (no la duplica ni depende de ella para existir primero — podría implementarse con su propia validación inline si hiciera falta); no depende de EP-001 más que de que el formulario de operación exista (mismo tipo de dependencia de plataforma ya aceptada en HU-008).
- [x] **N**egotiable — el diseño exacto del campo inline (tamaño, posición) queda abierto al equipo.
- [x] **V**aluable — sí: evita interrumpir el registro rápido de una operación (objetivo #1 del PRD) cuando falta una etiqueta.
- [x] **E**stimable — alcance acotado, reutiliza lógica ya validada de HU-010.
- [x] **S**mall — un campo inline + reutilización de lógica existente, cabe claramente en 2-4 días.
- [x] **T**estable — 4 escenarios en Given/When/Then con resultados observables (campo visible, selección automática, comportamiento del campo inline ante el error de duplicado sin revalidar la regla en sí, cierre sin crear). Ajustado tras `/trycore:invest`: el Escenario 3 original duplicaba cobertura de test con HU-010; ahora prueba solo el ángulo propio (foco, no-navegación, datos intactos).
