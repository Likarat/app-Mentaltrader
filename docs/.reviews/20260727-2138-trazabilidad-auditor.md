# Trazabilidad Audit — App Mentaltrader (Bitácora de Trading Forex) — 2026-07-27

**Artefactos verificados**: PRD (5 objetivos §1, 5 capabilities §5, 4 KPIs §12) · `epicas.md` (EP-001..EP-005) · 35 archivos `HU-XXX-*.md` · `backlog.md` · `moscow-2026-07-27.md`
**Veredicto**: la cadena **no tiene huérfanos sintácticos** (0 IDs inexistentes, 0 historias sin épica, 0 referenciadas sin archivo), pero hay **3 quiebres semánticos serios**.

## Cobertura top-down

| Objetivo PRD | Épicas | Historias reales | AC que codifican el umbral | Estado |
|---|---|---|---|---|
| O1 — registro ≤30s | EP-001 | HU-001…009 + HU-013 | **0** | 🔴 no verificable |
| O2 — historial 3.000–5.000 ops | EP-003 | HU-015, HU-016 + 9 más | 2 (HU-016) | 🟡 medible pero en historia Should |
| O3 — ranking patrones | EP-004(+002,003) | HU-026, HU-027 + 4 | n/a | ✓ completa |
| O4 — offline sin pérdida | EP-005 | HU-035 | 2 | ✓ completa |
| O5 — imágenes ~200-400KB | EP-001/002/005 | HU-006(Should), HU-007(**Could**), HU-014(Could) | 1 (HU-007) | 🔴 fuera del MVP |

## 🔴 Quiebre 1 (el más grave): 5 campos consumidos por 8 AC, creados por ninguna historia

PRD §5 y `epicas.md` prometen "formulario con los 16 campos". HU-001+002+003 solo cubren 10 + imagen (HU-006/007) = 11. Faltan 5: **Riesgo (%)**, **Resultado en R**, **emotionBeforeReason**, **emotionAfterReason**, **errorReason**.

HU-004 §Contexto admite explícitamente: *"campos opcionales del modelo `Operation` (Riesgo %, Resultado en R). Esta historia no agrega campos nuevos ni UI nueva"* — pero ninguna otra historia los agrega tampoco.

**AC que dependen de campos inexistentes**:
- HU-004 E1, E4 — Riesgo %, Resultado en R
- HU-015 E3, HU-017 E1/E4, HU-026 E1/E2 — Resultado en R (para display y métricas de R acumulado)
- HU-023 E1 — los 3 campos `*Reason` (búsqueda de texto)

Consecuencia: **O3 (ranking/patrones) y las métricas de EP-004 dependen de un campo que EP-001 nunca crea.**

**Acción**: agregar historia a EP-001 ("Registrar campos opcionales de contexto: Riesgo %, Resultado en R, motivos de emoción/error") o extender HU-001/HU-002 con notas técnicas corregidas.

## 🔴 Quiebre 2: Objetivo 1 (≤30s) sin ningún AC que lo verifique

El umbral aparece 8 veces en `docs/` (PRD, epicas.md, checklists INVEST de HU-005/008/013) pero nunca como un `Entonces` observable, a diferencia de O2 (HU-016 E1/E3) y O5 (HU-007 E1).

**Acción**: agregar un AC de performance a HU-001 o HU-005 con el umbral de 30s, mismo patrón que HU-016.

## 🔴 Quiebre 3: Objetivo 5 declarado en 3 épicas, entregado en ninguna del MVP

Solo HU-007 (Could) contiene el umbral real. HU-014 (Could) solo reporta, no controla. EP-005 no tiene ningún AC de almacenamiento (su justificación confunde memoria en runtime con tamaño en disco). El objetivo queda sin dueño ejecutable en el backlog priorizado.

**Acción**: subir HU-007 a Must, o registrar en el PRD que el objetivo se difiere post-v1. Revisar la reclamación EP-005→Obj.5 en `epicas.md`.

## 🟡 Hallazgos menores

- **HU-019** es la única historia sin bullet propio en PRD §5 — su único rastro es un paréntesis dentro de Capability 5 (EP-005). Rompe la afirmación "alineadas 1:1 con las épicas".
- **12/35 historias** (HU-009,011,012,018,019,020,021,024,025,032,033,034) terminan su cadena en PRD §5/§3 y nunca alcanzan un objetivo de §1 — no es huérfano (la épica sí cubre el objetivo) pero la transitividad historia→objetivo no es total y no está advertido.
- **HU-031 E1** depende de leer HU-025 (otra épica) para ser verificable — documentado y justificado, pero no auto-contenido.

## Referencias colgadas por renumeración (confirmadas, no corregidas)

- **HU-008 checklist INVEST**: dos referencias a "HU-028" que deberían ser "HU-032" (ya detectado por `invest-validator`, sigue sin corregirse).
- **HU-006 y HU-008**: mencionan "HU-030 en una numeración anterior" — hoy HU-030 es otra historia real (EP-004), colisión de ID viva en el texto.
- **`backlog.md` §Resumen**: dice "S 24, M 11" pero el conteo real de las 35 filas/frontmatter es **S 26, M 9**.

## Hallazgos de higiene documental

- `epicas.md` línea 182: dice "6 historias se dividieron" pero fueron 5 originales (→6 nuevas).
- `epicas.md` línea 3: nota "el PRD §5 está en TODO" desactualizada (§5 ya se completó).
- Título de HU-017 difiere entre `epicas.md` y frontmatter/backlog (solo orden de palabras, no ambigüedad real).
- `HU-010-administrar-catalogos.md` conserva el slug de la historia pre-división (título real: "Agregar elemento a un catálogo").
- `docs/02-user-story-map/` y `docs/06-flows/` vacíos — la cadena PRD→Backlog saltó el User Story Map.

## Reporte de IDs

Sin huérfanos sintácticos: 0 IDs de épica inexistentes, 0 historias sin épica, 0 historias referenciadas sin archivo. Distribución verificada: EP-001=9, EP-002=5, EP-003=11, EP-004=6, EP-005=4 = 35 ✓.

## Acciones recomendadas (top 5)

1. Cerrar el gap de los 5 campos faltantes (Quiebre 1) — el más urgente, afecta directamente a O3/EP-004.
2. Agregar AC de performance para el umbral de 30s (Quiebre 2).
3. Resolver O5: subir HU-007 a Must o diferir el objetivo explícitamente en el PRD (Quiebre 3).
4. Corregir referencias colgadas: HU-028→HU-032 en HU-008; S26/M9 en backlog.md; renombrar HU-010; "6→5 historias" en epicas.md.
5. Anclar HU-019 en PRD §5 Capability 3, o documentar el movimiento de capability en epicas.md.
