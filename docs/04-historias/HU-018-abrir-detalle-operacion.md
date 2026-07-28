---
id: HU-018
titulo: Abrir detalle completo de una operación desde la tarjeta
epica: EP-003
prioridad: Must
complejidad: S
estado: lista
---

# Abrir detalle completo de una operación desde la tarjeta

## Historia

Como **trader de forex**,
quiero **tocar una tarjeta del listado para abrir la vista de detalle completo de esa operación, con sus campos organizados en secciones legibles**,
para **estudiar en profundidad el contexto completo de esa operación**.

## Contexto

Se apoya en HU-015 (la tarjeta y el listado ya deben existir) y en los campos definidos por EP-001. Cubre la navegación a la vista de detalle y la estructura de secciones (imagen primero si existe, luego "Datos generales"/"Emociones"/"Resultado", Descripción entrada al final — mismo orden de lectura del formulario). El visor de imagen a pantalla completa con zoom y navegación entre imágenes se cubre en **HU-019**, no aquí. Editar y eliminar desde esta vista se cubren en **HU-020** y **HU-021** respectivamente.

## Criterios de aceptación

### Escenario 1 — Happy path: abrir detalle desde la tarjeta
- **Dado que** el usuario ve una tarjeta en el listado
- **Cuando** toca la tarjeta
- **Entonces** el sistema abre la vista de detalle completo de esa operación

### Escenario 2 — Happy path: ver todos los campos organizados en secciones legibles
- **Dado que** la vista de detalle de una operación con imagen carga
- **Cuando** el usuario la visualiza
- **Entonces** el sistema muestra primero la imagen, seguida de los campos organizados en secciones con encabezados claros, terminando con la Descripción entrada al final

### Escenario 3 — Edge: operación sin imagen adjunta
- **Dado que** una operación no tiene ninguna imagen adjunta
- **Cuando** el usuario abre su vista de detalle
- **Entonces** el sistema omite la sección de imagen sin dejar espacio vacío ni layout roto, mostrando el resto de las secciones con normalidad

## Notas técnicas (opcional)

- Mismo orden de lectura que el formulario de registro (spec §4, Requirement "Vista de detalle").

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-015 (mismo epic, tarjeta/listado ya debe existir), sin acoplarse a HU-019/020/021 (capas adicionales sobre esta vista base).
- [x] **N**egotiable — el diseño exacto de las secciones es negociable.
- [x] **V**aluable — sí: es el punto de entrada al estudio profundo de cada operación, propósito central del producto.
- [x] **E**stimable — alcance acotado a navegación + estructura de secciones.
- [x] **S**mall — cabe claramente en 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (vista abierta, orden de secciones, ausencia de imagen sin romper layout).
