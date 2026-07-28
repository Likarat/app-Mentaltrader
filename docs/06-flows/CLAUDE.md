# CLAUDE.md — docs/06-flows/

Contexto local para Claude cuando edites flujos de navegación. Reglas mínimas; el resto vive en `METODOLOGIA.md` §5a.

## Convención de archivos

Un archivo **por épica**: `EP-XXX-<slug>.md`. Granularidad atómica heredada del User Story Map.

## Reglas locales clave (rejection rules)

1. **Frontmatter YAML obligatorio**: `id, epica, historias_cubiertas`.
2. **Cada arco del diagrama mapea 1:1 a un AC existente**, anotado con `%% HU-XXX` inmediatamente antes del arco. Sin esa marca, el arco se considera inventado.
3. **Cobertura por flow**: happy path + ≥1 ramal de error + ≥1 edge case.
4. **Cobertura por épica**: toda HU con `epica: EP-XXX` aparece referenciada en el flow correspondiente.
5. **Actores válidos: solo los del PRD §Stakeholders**.
6. **Mermaid debe parsear** (`sequenceDiagram` o `flowchart TD`, no mezclar).

## Skills relevantes

- `trycore-mapear-flujos-navegacion` (opcional, post-pipeline).
- Agente revisor: `flows-auditor` (auto si `TRYCORE_AUTO_AUDIT=true`).

## Qué NO va aquí

Wireframes, imágenes, ASCII art. Mermaid puro es el contrato.
