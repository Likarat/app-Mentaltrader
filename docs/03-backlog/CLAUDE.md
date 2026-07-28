# CLAUDE.md — docs/03-backlog/

Contexto local para Claude cuando edites épicas o backlog. Reglas mínimas; el resto vive en `METODOLOGIA.md` §2 y §6.

## Dos artefactos en este directorio

- `epicas.md` — descomposición del PRD en épicas (`EP-XXX`) con trazabilidad bidireccional a objetivos del PRD.
- `backlog.md` — tabla ordenada de todas las historias. **El orden de filas ES la priorización vigente.**

## Reglas locales clave

1. **IDs `EP-XXX` únicos** (3 dígitos), sin saltar ni reusar números.
2. **Trazabilidad bidireccional**: cada épica → ≥1 objetivo del PRD; cada objetivo → ≥1 épica.
3. **El backlog NO se prioriza aquí** — la priorización vive en `docs/05-priorizacion/`. Esta carpeta solo consolida.

## Skills relevantes

- `trycore-descomponer-prd-a-epicas` (escribe `epicas.md`).
- `trycore-construir-backlog` (escribe `backlog.md`).
- Agente revisor: `story-decomposer-auditor` + `trazabilidad-auditor`.

## Qué NO va aquí

Historias individuales (van en `docs/04-historias/`).
