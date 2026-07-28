---
id: HU-014
titulo: Ver espacio en disco usado por imágenes
epica: EP-002
prioridad: Could
complejidad: S
estado: lista
---

# Ver espacio en disco usado por imágenes

## Historia

Como **trader de forex**,
quiero **ver el espacio total aproximado en disco que ocupan todas mis imágenes adjuntas al abrir la pestaña Etiquetas**,
para **tener visibilidad de cuánto almacenamiento está usando la app y decidir si necesito depurar operaciones antiguas**.

## Contexto

Última historia prevista de EP-002; con ella la épica queda completa. Requiere que existan imágenes adjuntas (HU-006/HU-007, EP-001) para tener datos reales que mostrar — dependencia de datos de prueba, no de alcance (mismo patrón que HU-012). Es una vista de solo lectura, agregada; no modifica ningún dato.

## Criterios de aceptación

### Escenario 1 — Happy path: mostrar el espacio total usado
- **Dado que** el usuario tiene una o más operaciones con imágenes adjuntas
- **Cuando** abre la pestaña de Etiquetas
- **Entonces** el sistema muestra el espacio total aproximado en disco ocupado por todas las imágenes

### Escenario 2 — Edge: ninguna imagen adjuntada todavía
- **Dado que** ninguna operación tiene imágenes adjuntas
- **Cuando** el usuario abre la pestaña de Etiquetas
- **Entonces** el sistema muestra el espacio usado como 0 (o equivalente), sin mostrar ningún error

### Escenario 3 — Edge: el cálculo se actualiza tras eliminar una operación con imágenes
- **Dado que** el usuario eliminó una operación que tenía imágenes adjuntas (y sus archivos fueron borrados)
- **Cuando** vuelve a abrir la pestaña de Etiquetas
- **Entonces** el espacio total mostrado refleja la reducción, sin contar los archivos ya eliminados

## Notas técnicas (opcional)

- Cálculo aproximado, no exacto al byte (spec: "espacio total aproximado").
- No requiere recalcularse en tiempo real fuera de la pestaña Etiquetas (solo al abrirla), evitando overhead innecesario.

## Checklist INVEST

- [x] **I**ndependent — sí: depende de HU-006/HU-007 (EP-001) solo para tener datos de prueba reales, no para su propio alcance (vista de solo lectura); no depende de ninguna otra historia de EP-002.
- [x] **N**egotiable — el formato exacto de presentación (MB/KB, texto vs. barra) queda abierto al equipo.
- [x] **V**aluable — sí: da visibilidad de consumo de almacenamiento, sostiene el objetivo #5 del PRD.
- [x] **E**stimable — alcance acotado a una consulta de agregación sobre `OperationImage`.
- [x] **S**mall — una consulta + un texto en la UI, cabe claramente en menos de 2-4 días.
- [x] **T**estable — 3 escenarios en Given/When/Then con resultados observables (valor mostrado, caso cero, recálculo tras eliminación).
