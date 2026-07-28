# CLAUDE.md — docs/04-historias/

Contexto local para Claude cuando edites Historias de Usuario y sus AC. Reglas mínimas; el resto vive en `METODOLOGIA.md` §3 y §4.

## Convención de archivos

Una historia por archivo: `HU-XXX-<slug>.md` (3 dígitos, kebab-case-sin-acentos).

## Reglas locales clave (no negociables)

1. **Frontmatter YAML obligatorio**: `id, titulo, epica, prioridad, complejidad, estado`. Estado inicial siempre `draft`.
2. **Formato canónico de historia**: `Como [rol específico], quiero [acción concreta], para [beneficio externo y visible]`. Rol genérico ("usuario") se rechaza.
3. **AC en Given/When/Then** (3-5 escenarios: happy + error + edge). Given = estado, When = una acción, Then = resultado observable.
4. **Pasa INVEST** antes de marcar `estado: lista`.

## Skills relevantes

- `trycore-escribir-historia-usuario` (estructura "Como/Quiero/Para").
- `trycore-escribir-criterios-aceptacion-bdd` (AC en G/W/T).
- `trycore-validar-invest` (validación de los 6 criterios).
- Agentes revisores: `invest-validator`, `bdd-validator`, `trazabilidad-auditor`.

## Qué NO va aquí

Épicas (en `docs/03-backlog/`), priorización (en `docs/05-priorizacion/`), flows (en `docs/06-flows/`).
