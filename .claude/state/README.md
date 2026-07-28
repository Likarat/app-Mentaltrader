# Estado del arnés de construcción (`build-state.json`)

`build-state.json` es la **única fuente de verdad** del slice (épica) en construcción y el medio por
el que los agentes se sincronizan en modo **secuencial**. La unidad de construcción es la **épica
(`EP-XXX`)**; las HU que cubre el change se listan en `hus[]` (trazabilidad al alcance interno).
Cada gate del pipeline transiciona el estado; el siguiente agente lo lee antes de actuar.

- **Esquema**: `build-state.schema.json` (versionado, draft 2020-12). Valida con:
  ```bash
  # Recomendado (funciona sin plugins de formato):
  python3 -c "import jsonschema,json; jsonschema.Draft202012Validator(json.load(open('.claude/state/build-state.schema.json'))).validate(json.load(open('.claude/state/build-state.json'))); print('OK')"
  # Alternativa con ajv (requiere spec + formatos):
  npx --yes ajv-cli@5 validate --spec=draft2020 -c ajv-formats -s .claude/state/build-state.schema.json -d .claude/state/build-state.json
  ```
- **No versionar el estado vivo**: `build-state.json` está en `.gitignore` (working state). El
  schema y este README sí se versionan.

## Protocolo de lectura/escritura

1. **Leer antes de actuar.** Todo agente del pipeline lee `active_slice` y `gates` antes de
   trabajar. Si `active_slice` es `null`, no hay slice en curso → solo `dor-dod-gatekeeper` puede
   abrir uno.
2. **Una sola escritura por transición.** Quien completa una fase actualiza `phase`, el gate
   correspondiente, `updated_at` (ISO 8601 UTC) y `updated_by` (su nombre). No tocar otros campos.
3. **Gates monótonos hacia adelante.** Un gate solo pasa de `false`→`true` cuando su agente lo
   aprueba. Si una revisión posterior falla, se vuelve a `false` y `phase` retrocede.
4. **`fidelity`/`api` admiten `null`** cuando el slice no tiene UI o endpoints (N/A, no bloquea DoD).
   `fidelity` **no** puede quedar `null` si el slice toca UI. `wiring_verified` aplica siempre (sin `null`)
   y es **prerequisito de `dod`**.
5. **Archivar**: al completar `opsx:archive`, mover el `active_slice` a `history[]` con
   `phase: "archived"` y dejar `active_slice: null`.
6. **Dos loops**: el slice (inner loop) cierra gates baratos por épica; los gates pesados viven en
   `releases[]` (outer loop), que escribe la skill `releasing-a-version` una vez por release.

## Ciclo de fases

**Inner loop (por slice):**
```
dor → change → red → green → refactor → smoke → api → data → dod → pr → archived
```
Luego se pregunta el **Release Gate** (outer loop, skill `releasing-a-version`) con default
computado desde las líneas de release del Story Map.

`harness_phase` arranca en `authoring` y lo cambia `load-build-state.sh` a `active` cuando
detecta `package.json` en la raíz (aparición del scaffold de código del proyecto). Es una señal
**informativa**, no el gate de avance.

### Gate `scaffold` (Paso 1 fundamental)

`scaffold` es un gate **de proyecto** (no por-slice): `{ confirmed, confirmed_by, confirmed_at, notes }`.
Arranca en `confirmed: false` y **solo** pasa a `true` por **confirmación explícita** (vía
`building-a-slice` Fase 0 / `dor-dod-gatekeeper`), nunca por auto-detección. Es la precondición
restrictiva para abrir cualquier slice: el hook `scaffold-guard.sh` bloquea escribir código de
slice (fases `red…data`) mientras `confirmed` no sea `true`. El arnés **no genera** el scaffold.

### `design_source` (gate de proyecto, slices con UI)

Espejo de `scaffold` para la UI: `applies` (¿el proyecto tiene UI?), `confirmed` (humano confirmó que
existe una fuente de diseño declarada), `source` (puntero al prototipo/export). El arnés NO genera el
prototipo. Lo respalda `design-source-guard.sh`.

### gate `fidelity` (por-slice, inner loop) — ESTRICTO para UI

`true` = FIEL (o desviaciones justificadas, **vía verificación visual real**); `false` = desviaciones
sin justificar **o** verificación visual no realizada; `null` = slice sin UI. Lo computa
`ux-fidelity-reviewer` en la fase `smoke` y lo escribe el `build-orchestrator`. Para slices con UI
(`design_source.applies===true`) solo cierra con **MCP chrome-devtools** (screenshot app vs prototipo):
INCONCLUSO ya **no** pasa (queda `false` → el `dod` no cierra hasta correrlo donde haya MCP).

### Handoff fino del cableado (`wiring_checklist[]`, `progress_log[]`, `sub_slices[]`) + gate `wiring_verified`

Campos por-slice que hacen el **refresh de contexto el estado por defecto** (una sesión fresca retoma
desde disco, no desde la conversación):
- **`wiring_checklist[]`** — un item por escenario AC y por punto de integración entre capas; nace
  `failing`, pasa a `passing` **solo tras prueba real ejecutada** (con `evidence`).
- **`progress_log[]`** — bitácora append-only (`{at, by, note}`) que sobrevive al reset.
- **`sub_slices[]`** — descomposición de una épica que superó el gate de tamaño (>3 HU ó ≥3 capas).
- **gate `wiring_verified`** — lo cierra el `wiring-adversarial-verifier` (subagente **independiente**,
  contexto virgen, que intenta **refutar** el slice). `dod` no puede ser `true` sin él. Lo inicializa
  el `dor-dod-gatekeeper` en `false` durante el DoR.

### Reflexión post-slice (ciclo autocorrectivo)

Tras archivar un slice, su entrada en `history[]` puede llevar `reflected` / `reflected_at`. El
hook `reflect-nudge.sh` (evento `Stop`, **no bloqueante**) sugiere ejecutar `/build:reflect` mientras
exista al menos una entrada con `reflected != true`. `/build:reflect` lo ejecuta el **modelo**:
detecta convención nueva o error recurrente, **propone** un parche al bloque `trycore-build-learnings`
de `CLAUDE.md` (se aplica **solo tras tu aprobación**) y estampa `reflected: true` → el nudge calla.
El razonamiento vive en el modelo; el hook solo es un recordatorio determinista.

## Quién escribe qué

| Campo / gate | Lo escribe | Cadencia |
|---|---|---|
| `active_slice` (alta) · `gates.dor` · `gates.dod` | `dor-dod-gatekeeper` | slice |
| `gates.coherence_link` | `change-epic-coherence` | slice |
| `gates.tdd` | flujo `superpowers:test-driven-development` (vía `build-orchestrator`) | slice |
| `gates.journey_smoke` · `gates.fidelity` · `phase` (transiciones) · `history[]` (archivado) · `wiring_checklist[]` · `progress_log[]` · `sub_slices[]` | `build-orchestrator` (fidelity desde `ux-fidelity-reviewer`) | slice |
| `gates.api` | `api-contract-tester` | slice |
| `gates.data` | `data-consistency-checker` | slice |
| `gates.wiring_verified` | `wiring-adversarial-verifier` (independiente, contexto virgen) | slice (antes de `dod`) |
| `releases[]` (`security`, `smell`, `ux`, `coherence`, `stack_arch`, `integration`, `status`) | `releasing-a-version` (delega en `security-reviewer`, `simple-design-reviewer`, `ux-krug-reviewer`, `coherence-three-way`, `stack-guardian`) | release |
| `history[].reflected` · `history[].reflected_at` | `/build:reflect` | post-slice (tras archivar) |
| `harness_phase` | `load-build-state.sh` (SessionStart) | — |
