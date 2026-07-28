# CLAUDE.md

<!-- BEGIN trycore-vertical v0.4.0 -->
## Vertical documentaria Trycore

Este proyecto usa la vertical Trycore (`@trycore/spec-product-flow`) para producir y mantener artefactos de discovery / producto:

```
PRD → User Story Map → Backlog → Historias (AC G/W/T) → Priorización → Flows
```

### Estructura

```
docs/
├── 01-prd/                 PRD (12 componentes obligatorios)
├── 02-user-story-map/      Mapa estilo Jeff Patton (backbone + ranking + releases)
├── 03-backlog/             epicas.md + backlog.md (tabla ordenada)
├── 04-historias/           HU-XXX.md con frontmatter YAML obligatorio + AC en G/W/T
├── 05-priorizacion/        Aplicación de framework (MoSCoW/RICE/Valor-Esfuerzo/Eisenhower)
└── 06-flows/               Flujos de navegación en Mermaid, un archivo por épica
```

### Reglas duras (no negociables)

1. Toda Historia de Usuario vive en `docs/04-historias/HU-XXX.md` con frontmatter YAML obligatorio (`id, titulo, epica, prioridad, complejidad, estado`).
2. Todo Acceptance Criterion se escribe en **Given/When/Then**; nada de prosa libre. 3-5 escenarios incluyendo happy / error / edge.
3. Toda Historia pasa los 6 criterios **INVEST** antes de marcarse `estado: lista`.
4. La metodología canónica vive en el `METODOLOGIA.md` del paquete `@trycore/spec-product-flow` (ruta exacta depende de dónde npm haya instalado el paquete global; típicamente `$(npm root -g)/@trycore/spec-product-flow/METODOLOGIA.md`). Si una skill contradice la metodología, **gana la metodología**.
5. Ningún artefacto se marca como "final" sin pasar por su agente revisor (manual con `/trycore:revisar` o automático si `TRYCORE_AUTO_AUDIT=true`).

### Slash commands disponibles

| Operativos | Por artefacto |
|---|---|
| `/trycore:onboard` — parametriza el proyecto | `/trycore:prd` — escribe PRD |
| `/trycore:flujo` — pipeline completo | `/trycore:epicas` — descompone PRD |
| `/trycore:revisar` — auditoría global | `/trycore:mapa` — User Story Map |
|  | `/trycore:historia` — historia de usuario |
|  | `/trycore:ac` — criterios de aceptación BDD |
|  | `/trycore:invest` — valida INVEST |
|  | `/trycore:backlog` — backlog consolidado |
|  | `/trycore:priorizar` — priorización |
|  | `/trycore:flows` — flujos de navegación (Mermaid, por épica) |

### Contexto del proyecto

- **Nombre**: {{PROJECT_NAME}}
- **Dominio**: {{DOMAIN_HINT}}
- **Stakeholders**: {{STAKEHOLDERS}}
- **Framework de priorización**: {{PRIORITIZATION_FRAMEWORK}}

(Si los valores aparecen como `{{...}}` aún, ejecuta `/trycore:onboard` para parametrizar.)

### Convenciones de naming

- Épicas: `EP-001`, `EP-002`, … (3 dígitos)
- Historias: `HU-001`, `HU-002`, … (3 dígitos)
- Slugs en kebab-case-sin-acentos

### Hooks de calidad (opt-in)

Dos flags opcionales en `.claude/settings.local.json`:

```json
{
  "env": {
    "TRYCORE_AUTO_AUDIT": "true",
    "TRYCORE_REFLECT_SESSION": "true"
  }
}
```

- `TRYCORE_AUTO_AUDIT=true` → tras cada Write/Edit en `docs/`, dispara el agente revisor correspondiente. Cuando esté off (default), usar `/trycore:revisar` manualmente.
- `TRYCORE_REFLECT_SESSION=true` → al cerrar sesión, escanea heurísticas (campos no canónicos en frontmatter, AC fuera de G/W/T, secciones inesperadas en PRD) y escribe propuestas en `docs/.trycore-suggestions.md` sin auto-editar nada. Útil para capturar convenciones emergentes del cliente.

<!-- END trycore-vertical -->

<!-- A partir de aquí, el equipo del proyecto puede agregar instrucciones específicas del cliente. -->

<!-- BEGIN trycore-build-harness v0.8.1 -->
## Arnés de construcción (Trycore Build Harness)

Este proyecto usa el arnés `@trycore/spec-build-harness` para **construir** sobre la discovery
(compañero de `@trycore/spec-product-flow`: Discovery → Construcción). Modelo de **dos loops**:

```
Inner loop (por épica EP-XXX):  DoR → OpenSpec change → TDD → journey-smoke → api/data → verificación adversarial → DoD → PR + archive
Outer loop (por release):       Release Gate (seguridad · diseño · UX · coherencia triple · arquitectura · integración)
```

- **Unidad de construcción**: la épica (`EP-XXX`). Un slice = una épica = un OpenSpec change = una rama = un PR.
- **Una sola fuente de verdad**: `.claude/state/build-state.json` (schema + protocolo en `.claude/state/README.md`).
- **Skills**: `building-a-slice` (inner loop), `releasing-a-version` (outer loop), `building-a-micro-change` (carril de mantenimiento), `openspec-*` (ciclo de changes).
- **Agentes** en `.claude/agents/build/`; **comandos** `/build:*` + `/opsx:*`; gates automatizados por **hooks** en `.claude/hooks/build/`.

### Slash commands

| Comando | Propósito |
|---|---|
| `/build:work` | Router (classify-and-act): enruta el trabajo a micro-change / slice / release |
| `/build:slice` | Abre/continúa un slice (épica `EP-XXX`) — entrada del inner loop |
| `/build:release` | Corre el Release Gate (outer loop) sobre el diff acumulado de la release |
| `/build:onboard` | Parametriza el dominio del arnés (rellena el bloque de dominio de abajo) y escribe memoria |
| `/build:reflect` | Reflexión post-slice: propone aprendizajes a `CLAUDE.md` (solo con tu aprobación) |
| `/opsx:explore` `/opsx:new` `/opsx:continue` `/opsx:apply` | Ciclo OpenSpec: explorar → crear change → artefactos → implementar |
| `/opsx:verify` `/opsx:archive` `/opsx:sync` `/opsx:ff` `/opsx:bulk-archive` | Verificar, archivar, sincronizar specs |

### Reglas duras (no negociables)

1. **GitHub Flow estricto**: `main` siempre desplegable; integración solo por PR (el hook `gitflow-guard.sh` bloquea commit/push directo a `main`).
2. El enlace change↔épica va en el bloque `## Trazabilidad` del `proposal.md`, **nunca** en frontmatter YAML (rompe `openspec validate`).
3. Las dependencias se vigilan contra `.claude/config/stack-allowlist.json` (hook `stack-guard.sh`), derivado de la sección de requisitos técnicos del PRD.
4. Las revisiones pesadas (seguridad/diseño/UX/coherencia triple/arquitectura/integración) corren **una vez por release** (outer loop), no por épica.
5. **Producto completo, no MVP.** El alcance acordado se construye **entero**. **Recortar o diferir es bloqueante explícito** que requiere acuerdo del equipo — **nunca** una decisión del modelo. No se "deja para después" ni se deriva en lo complejo. La verificación es **ejecutada, no por inspección** (correr la suite, cargar la página, leer la consola).
6. **Cierre verificado, no declarado.** `dod` exige el gate `wiring_verified`: un subagente **adversarial independiente** (`wiring-adversarial-verifier`, contexto virgen) intenta refutar el slice (stubs, rutas sin cablear, AC sin test) antes de cerrar. El estado del cableado vive en disco (`wiring_checklist[]` + `progress_log[]`) para que una sesión fresca retome sin "creer que ya está".
7. **Fidelidad por verificación visual real.** Para slices con UI, el gate `fidelity` solo cierra observando la salida real vía MCP de devtools de navegador (screenshot app vs prototipo); sin verificación visual queda `false` (no "INCONCLUSO pasa").
8. **Cimiento antes que negocio y unidades pequeñas.** Las épicas de cimiento (auth, datos, arquitectura base, design-system) se construyen antes que las de negocio; una épica grande (>3 HU ó ≥3 capas) se descompone en sub-slices construidos de a uno.
9. Si una regla del arnés contradice la metodología Trycore (`METODOLOGIA.md`), **gana la metodología**.

### Bloque de dominio (lo resuelve `/build:onboard`)

Estos puntos de extensión los leen los agentes `security-reviewer`, `stack-guardian`,
`data-consistency-checker`, `ux-krug-reviewer`, `simple-design-reviewer` y `ux-fidelity-reviewer`:

- **PRD técnico (fuente del stack)**: {{PRD_TECH_PATH}}
- **Capa de servicios externos / IA (frontera)**: {{EXTERNAL_SERVICE_LAYER}}
- **Lógica que debe ser determinista (no-IA)**: {{DETERMINISTIC_LAYER}}
- **Categorías de datos sensibles / PII reguladas**: {{SENSITIVE_DATA_CATEGORIES}}
- **Secretos server-side**: {{SERVER_SIDE_SECRETS}}
- **Decisiones de alto impacto que exigen explicabilidad UX**: {{HIGH_STAKES_DECISIONS}}
- **Fuente de diseño / referencia visual**: {{DESIGN_SOURCE}}

(Si aparecen como `{{...}}`, ejecuta `/build:onboard` para parametrizarlos.)

### Requisitos

- `openspec` CLI instalado (`npm i -g @fission-ai/openspec`) — lo usan `/opsx:*` y las skills `openspec-*`.
- `python3` y `git` disponibles — los usan los hooks de `.claude/hooks/build/`.
- Verifica con `trycore-build doctor`.

<!-- END trycore-build-harness -->

<!-- BEGIN trycore-build-learnings -->
## Convenciones aprendidas (mantenido por /build:reflect)

<!-- /build:reflect propone aquí viñetas concretas (convención nueva o error recurrente) tras cerrar un slice; se agregan SOLO con tu aprobación. Revísalas en PR como cualquier cambio de equipo. -->
<!-- END trycore-build-learnings -->
