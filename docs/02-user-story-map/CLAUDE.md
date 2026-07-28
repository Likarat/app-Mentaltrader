# CLAUDE.md — docs/02-user-story-map/

Contexto local para Claude cuando edites el User Story Map. Reglas mínimas; el resto vive en `METODOLOGIA.md` §5.

## Convención de archivos

- Un archivo: `<slug>.md` (mismo slug que el PRD).
- Formato preferido: tabla Markdown (más portable que Mermaid para Story Maps).

## Reglas locales clave

1. **Backbone cubre el journey completo** del usuario primario. Sin saltos.
2. **Línea de MVP explícita** — sin MVP definido, el mapa no entrega valor.
3. **Historias referenciadas por ID** (`HU-XXX`). IDs reservados si la historia aún no existe.

## Skills relevantes

- `trycore-crear-mapa-historias` (después de tener épicas).
- Agente revisor: `mapping-coherence-auditor`.

## Qué NO va aquí

Detalle de historias (van en `docs/04-historias/`) ni AC.
