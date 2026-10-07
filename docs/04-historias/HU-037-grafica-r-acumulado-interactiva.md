---
id: HU-037
titulo: Explorar la gráfica de R acumulado tocando y arrastrando sobre un punto
epica: EP-004
prioridad: Should
complejidad: M
estado: lista
---

# Explorar la gráfica de R acumulado tocando y arrastrando sobre un punto

## Historia

Como **trader de forex**,
quiero **tocar y arrastrar el dedo sobre la gráfica de R acumulado y ver la fecha y el valor de R del punto más cercano**,
para **saber exactamente cuánto R tenía en un momento concreto, sin tener que inferirlo a ojo desde la línea**.

## Contexto

Extiende la gráfica de R acumulado ya cubierta por **HU-026** (Escenario 2) dentro de EP-004. La gráfica (`RAcumuladoLineChart`, `InicioCharts.kt`) está dibujada a mano con Compose `Canvas` puro — el stack no admite hoy ninguna librería de gráficas (ver `stack-allowlist.json`) — así que la interacción de toque/arrastre y el tooltip se implementan sobre el mismo Canvas, sin componentes de terceros. La fuente de datos es la serie ya existente `ResultInRCumulativeSeries.Point` (fecha + R acumulado por punto), expuesta por `InicioViewModel.cumulativeSeries`; esta historia no cambia esa serie, solo agrega una forma de leerla interactivamente.

No cubre el caso de "menos de 2 puntos" (gráfica no se dibuja, ver HU-031 estado vacío) — ahí no hay nada que arrastrar.

## Criterios de aceptación

### Escenario 1 — Happy path: arrastre muestra fecha y R del punto más cercano
- **Dado que** la gráfica de R acumulado está visible con al menos 2 puntos de datos
- **Cuando** el usuario presiona sobre la gráfica y arrastra el dedo horizontalmente
- **Entonces** el sistema muestra, en tiempo real, un indicador visual (línea vertical + tooltip) sobre el punto de datos más cercano a la posición del dedo, con su fecha y su valor de R acumulado formateado con signo (mismo formato que `InicioFormatting.signedR`)

### Escenario 2 — Happy path: el indicador sigue el arrastre de punto en punto
- **Dado que** el usuario ya está arrastrando el dedo sobre la gráfica con el indicador visible en un punto
- **Cuando** mueve el dedo hacia otra posición horizontal dentro de la gráfica
- **Entonces** el indicador se actualiza al nuevo punto más cercano sin saltos ni parpadeos perceptibles, mostrando la fecha y el R de ese nuevo punto

### Escenario 3 — Happy path: soltar el dedo fija el último punto tocado
- **Dado que** el usuario está arrastrando el dedo con el indicador visible sobre un punto
- **Cuando** levanta el dedo de la pantalla
- **Entonces** el indicador permanece visible fijo en el último punto tocado, hasta que el usuario toque en cualquier lugar de la gráfica (sin arrastrar) o inicie un nuevo arrastre

> **Nota de alcance (confirmada con el usuario, 2026-10-07)**: el indicador se limpia con un toque
> simple **dentro de la propia gráfica** (no en cualquier parte de la pantalla fuera de ella, como
> decía una redacción anterior de este escenario). El usuario siempre tiene una forma de limpiarlo
> sin iniciar un nuevo arrastre; se acotó la superficie de detección a la gráfica misma.

### Escenario 4 — Edge: arrastre fuera de los límites horizontales de la gráfica
- **Dado que** el usuario está arrastrando el dedo sobre la gráfica
- **Cuando** el dedo sale del área horizontal donde hay puntos dibujados (antes del primer punto o después del último)
- **Entonces** el indicador se mantiene fijo (clamped) en el primer o último punto de la serie, según corresponda, sin mostrar datos fuera de rango ni desaparecer

### Escenario 5 — Edge: serie con exactamente 2 puntos
- **Dado que** la gráfica tiene el mínimo de datos necesario para dibujarse (exactamente 2 puntos)
- **Cuando** el usuario arrastra el dedo sobre la gráfica
- **Entonces** el indicador alterna correctamente entre esos 2 puntos según la posición del dedo, sin errores

## Notas técnicas (opcional)

- Hit-testing manual: mapear la posición X del gesto al punto de datos más cercano usando la misma escala que ya usa `RAcumuladoLineChart` para dibujar la polilínea.
- Implementar con `Modifier.pointerInput` (`detectDragGestures` o equivalente) sobre el `Canvas` existente; no se agrega ninguna dependencia nueva (coherente con `stack-allowlist.json`).
- El tooltip es responsabilidad visual de este Canvas (overlay dibujado o `Box` superpuesto); no reutiliza ningún componente de Material a menos que ya exista uno equivalente en el proyecto.

## Checklist INVEST

- [x] **I**ndependent — sí: solo depende de que HU-026 ya exista (la gráfica y su fuente de datos), no bloquea ni es bloqueada por otra historia pendiente.
- [x] **N**egotiable — el estilo exacto del indicador (línea + burbuja vs. solo burbuja) y si se fija o desaparece al soltar son detalles de diseño negociables; el AC fija el comportamiento mínimo (Escenario 3) pero no la apariencia.
- [x] **V**aluable — sí: responde directamente al pedido del usuario de saber "cuántas R tenía en ese momento", sin inferir a ojo.
- [x] **E**stimable — hit-testing sobre una serie ya existente y un overlay de tooltip; alcance acotado a un solo componente (`RAcumuladoLineChart`).
- [x] **S**mall — una sola capa (presentación/UI), un solo componente Canvas, cabe en un slice corto.
- [x] **T**estable — 5 escenarios en Given/When/Then con resultados observables (indicador visible, valor correcto, clamping en bordes, comportamiento con 2 puntos).

> OpenSpec change: `inicio-grafica-interactiva-y-alternador-resumen`.
