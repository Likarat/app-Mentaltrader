# docs/04-historias/

Una historia por archivo: `HU-001-<slug>.md`, `HU-002-<slug>.md`, …

Generar con `/trycore:historia` (formato "Como/Quiero/Para") + `/trycore:ac` (AC en G/W/T).

**Reglas**:
1. Frontmatter YAML obligatorio (`id, titulo, epica, prioridad, complejidad, estado`).
2. AC en Given/When/Then. 3-5 escenarios incluyendo happy, error, edge.
3. Pasa INVEST antes de `estado: lista` (validar con `/trycore:invest`).

Ver `METODOLOGIA.md` §3 (historia) y §4 (AC).
