# Story Decomposer Audit — 2026-07-27

**PRD**: `docs/01-prd/bitacora-trading-forex.md`
**Épicas**: `docs/03-backlog/epicas.md`

## Verificación de existencia

Ambos artefactos existen y tienen contenido real. PRD en modo one-pager (componentes 1,2,3,5,12) — no bloqueante, la sección 1 (objetivos) y 5 (capabilities) están completas.

## Matriz Épica × Objetivo-PRD (verificada contra contenido real)

|        | Obj 1 (≤30s) | Obj 2 (historial/scroll) | Obj 3 (ranking/patrones) | Obj 4 (offline) | Obj 5 (almacenamiento) |
|--------|:---:|:---:|:---:|:---:|:---:|
| EP-001 | ✓ directo |  |  |  | ✓ directo |
| EP-002 |  |  | ~ indirecto |  | ~ indirecto |
| EP-003 |  | ✓ directo | ~ secundario |  |  |
| EP-004 |  |  | ✓ directo |  |  |
| EP-005 |  |  |  | ✓ directo | ~ conceptualmente distinto |

Los 5 objetivos tienen ≥1 épica y las 5 épicas tienen ≥1 objetivo. **Sin bloqueante de cobertura bidireccional.** Pero varios enlaces marcados "✓" en el mapa son de soporte/indirectos, no implementación directa.

## Issues

### 🟡 Mayor: enlaces Objetivo↔Épica marcados como equivalentes cuando son de distinta fuerza

- **EP-002 → Obj. 3**: la justificación dice que catálogos limpios son "condición necesaria" para el ranking (EP-004) — dependencia habilitante, no implementación del objetivo. EP-002 no expone ningún ranking.
- **EP-002 → Obj. 5**: mostrar espacio en disco no controla el consumo, solo lo reporta; el mecanismo real (compresión) vive en EP-001 (HU-007).
- **EP-005 → Obj. 5**: la justificación invoca eficiencia de memoria en runtime, conceptualmente distinto del objetivo real (tamaño en disco de imágenes comprimidas). Superposición de vocabulario ("almacenamiento") más que relación funcional real.

No genera gap de cobertura (Obj. 3 y 5 ya tienen dueño directo fuerte en EP-004 y EP-001 respectivamente), pero el mapa sobre-representa la trazabilidad de EP-002/EP-005. Recomendado: distinguir en la tabla cobertura directa vs. contribución/habilitante.

### 🟡 Mayor: métrica de éxito de EP-004 no es medible, a diferencia del resto

Las métricas de EP-001/002/003/005 son medibles (≤30s, 0 duplicados, 3,000-5,000 ops, 0 incidentes/100%). La de EP-004 ("el ranking se recalcula correctamente... sin intervención manual") es el antipatrón vago de "funciona bien". Causa raíz: el PRD §12 no tiene ningún KPI que ancle el objetivo 3.

**Fix sugerido**: "el ranking generado para un periodo coincide al 100% con un cálculo de referencia (script/test) sobre un dataset sintético, para los 5 tipos de periodo".

### 🟢 Menor: desbalance de granularidad

EP-003 tiene 11 historias (HU-015 a HU-025) vs. 4-9 en el resto — mezcla listado/agrupación + tarjeta + detalle + visor de imagen + edición/eliminación + filtros/búsqueda. Considerar partir en dos épicas en una futura iteración (filtros/búsqueda es bastante autocontenida). No bloqueante.

## Verificación de las dos resoluciones de solape documentadas

1. **Permisos de cámara (EP-005 → HU-006/EP-001)**: consistente, sin duplicación ni vacío. Confirmado en ambos lados.
2. **Visor de imagen (gap → HU-019/EP-003)**: ambos lados del cross-reference (EP-003 lo reclama, EP-005 cede la excepción) son mutuamente consistentes. Sin huérfanos ni duplicados.

🟢 Nota menor: PRD §5 dice que las capabilities están "alineadas 1:1 con las épicas", pero estas dos resoluciones mueven ítems fuera de su Capability 5 nominal. Está documentado y justificado, pero la frase "1:1" ya no es literalmente exacta.

## Sin hallazgos

IDs secuenciales sin saltos ni duplicados. Sin solapes de capability más allá de los dos ya resueltos.

## Acciones recomendadas

1. Distinguir cobertura directa vs. contribución/soporte en la tabla de trazabilidad (EP-002→Obj.3, EP-002→Obj.5, EP-005→Obj.5).
2. Reescribir la métrica de éxito de EP-004 con umbral verificable.
3. Evaluar partir EP-003 en dos épicas en una futura iteración.
4. Opcional: nota al pie en PRD §5 aclarando las dos excepciones al alineamiento "1:1".
