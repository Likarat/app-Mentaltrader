# CLAUDE.md — docs/05-priorizacion/

Contexto local para Claude cuando edites sesiones de priorización. Reglas mínimas; el resto vive en `METODOLOGIA.md` §7.

## Convención de archivos

Una sesión por archivo: `<framework>-YYYY-MM-DD.md` (ej. `moscow-2026-05-20.md`).

Frameworks soportados: **MoSCoW, RICE, Valor-Esfuerzo, Eisenhower**.

## Reglas locales clave

1. **Framework aplicado COMPLETO**, no parcial. Toda historia se clasifica.
2. **Distribución sana**:
   - MoSCoW: Must ≤ 60% del esfuerzo total. Si supera → advertir y reabrir discusión.
   - RICE: top 3 con Score ≥ 2× el promedio. Si no, el ordenamiento no aporta señal.
3. **Disidencias registradas** en sección dedicada del archivo.
4. **`backlog.md` se actualiza solo con aprobación explícita** del usuario (no automático).

## Skills relevantes

- `trycore-priorizar-backlog` (opcional, post-pipeline base).
- Agente revisor: `priorizacion-auditor`.

## Qué NO va aquí

El backlog en sí (vive en `docs/03-backlog/backlog.md`); aquí solo viven las sesiones que justifican su orden.
