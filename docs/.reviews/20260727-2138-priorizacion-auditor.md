# Auditoría de Priorización — App Mentaltrader

**Sesión auditada**: `docs/05-priorizacion/moscow-2026-07-27.md`
**Framework**: MoSCoW
**Backlog cotejado**: `docs/03-backlog/backlog.md`

## 1. Consistencia del framework MoSCoW

Conteo verificado manualmente:

| Categoría | Conteo manual | % declarado | % real |
|---|---|---|---|
| Must | 14 | 40% | 40.0% ✓ |
| Should | 12 | 34% | 34.3% ✓ |
| Could | 9 | 26% | 25.7% ✓ |
| Won't | 0 | 0% | 0% ✓ |

Total 35, todas clasificadas. Porcentajes aritméticamente correctos. El criterio operativo ("¿sin esto el producto falla como MVP usable?") se aplica con razonamiento defendible caso por caso, incluyendo casos límite bien justificados.

**Observación menor**: el encabezado dice "Distribución del **esfuerzo**" pero es un conteo de historias, no ponderación por complejidad (S/M/L). Ponderando S=1/M=2: Must = 41.3% — la conclusión no cambia (sigue ≤60%), pero el término es impreciso.

## 2. Distribución sana

Must 40% ≤ 60% ✓ — no se dispara alerta.

## 3. Coherencia con backlog.md

Mismo conjunto exacto de IDs por categoría en ambos documentos (Must, Should, Could). Ninguna historia desalineada. El orden de filas de `backlog.md` antepone HU-032/HU-033 como infraestructura base dentro de Must, decisión ya justificada en "Decisiones tomadas" de la sesión de priorización. Coherente.

## 4. Orden vs. dependencias declaradas en "Notas"

Revisadas las 35 filas: ninguna historia aparece antes que aquella de la que depende, dentro de su categoría. Sin violaciones de orden.

## 5. Cuadrantes sospechosos

"Won't = 0%" está justificado (Non-goals ya filtrados en fase de PRD/épicas). Válido para un proyecto pequeño, pero vale una pregunta abierta en la próxima revisión: ¿alguna historia Could (ej. HU-014, HU-016) es candidata a "Won't por ahora"?

## 6. Disidencias y próxima revisión

Ambas correctamente registradas en el documento (HU-006/007 con nota de reclasificación futura; próxima revisión atada a un hito, no solo fecha).

## Conclusión

Sin issues Mayores ni Críticos. Consistente internamente y sincronizado con `backlog.md`.

1. 🟢 Menor: precisar que "esfuerzo" en realidad es conteo de historias, no ponderación por complejidad.
2. 🟢 Menor: revisar en la próxima sesión si alguna historia Could debería bajar a "Won't por ahora".
