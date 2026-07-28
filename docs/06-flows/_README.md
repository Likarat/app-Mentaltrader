# docs/06-flows/

Un archivo por épica: `EP-001-<slug>.md`, `EP-002-<slug>.md`, …

Generar con `/trycore:flows` después de tener PRD + épicas + HU + Story Map. La skill `trycore-mapear-flujos-navegacion` produce un flujo en Mermaid (`sequenceDiagram` o `flowchart TD`) por cada épica, con trazabilidad arco↔HU↔AC.

**Reglas**:
1. Frontmatter YAML obligatorio (`id, epica, historias_cubiertas`).
2. Cada arco/transición del diagrama lleva `%% HU-XXX` como comentario inmediato.
3. Cobertura mínima por flow: happy path + ≥1 ramal de error + ≥1 edge case.
4. Toda HU declarada con `epica: EP-XXX` en su frontmatter debe aparecer en el flow correspondiente.
5. Actores válidos: solo los del PRD §Stakeholders.
6. Mermaid debe parsear (`sequenceDiagram` o `flowchart TD`, no otras variantes).

Auditar con `/trycore:revisar` — el agente `flows-auditor` verifica integridad estructural y trazabilidad bottom-up/top-down.

Ver `METODOLOGIA.md` §5a.
