# CLAUDE.md — docs/01-prd/

Contexto local para Claude cuando edites archivos en este directorio. Reglas mínimas; el resto vive en `METODOLOGIA.md` §1.

## Convención de archivos

- Un archivo por PRD vigente: `<slug>.md` (kebab-case-sin-acentos).
- Si hay módulos independientes: `prd-modulo-A.md`, `prd-modulo-B.md`.

## Regla local clave

Todo PRD debe tener **los 12 componentes obligatorios** (10 clásicos + Non-goals + KPIs). Variante One-Pager permitida en fase temprana; entrega final exige los 12.

## Skills relevantes en este directorio

- `trycore-escribir-prd` (crear/editar PRD)
- Agente revisor: `prd-reviewer` (vía `/trycore:revisar`)

## Qué NO va aquí

Épicas (van en `docs/03-backlog/`), historias (en `docs/04-historias/`), flows (en `docs/06-flows/`).
