#!/usr/bin/env bash
# design-source-guard.sh — PreToolUse · Write/Edit/MultiEdit
# Backstop determinista del "seguro de fuente de diseño": no se escribe código de un slice
# CON UI sin que la fuente de diseño del proyecto esté confirmada. Espejo de scaffold-guard.sh.
# Bloquea (exit 2) SOLO si: hay active_slice en fase de código (red/green/refactor/smoke/api/data),
# el proyecto tiene UI (design_source.applies===true), el slice está marcado UI-pendiente
# (gates.fidelity===false) y design_source.confirmed!==true.
# AUTO-ARME: si no existe build-state.json, exit 0.
set -uo pipefail

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
STATE="$ROOT/.claude/state/build-state.json"
[ -f "$STATE" ] || exit 0

INPUT="$(cat)"  # consumir stdin (protocolo de hook); la decisión es por estado.

# Guarda python [H4]: si falta, no podemos leer el estado de forma fiable. Fail-closed.
if ! command -v python >/dev/null 2>&1; then
  echo "⛔ design-source-guard: python no disponible; no puedo verificar el gate de fuente de diseño. Instala python (trycore-build doctor)." >&2
  exit 2
fi

VERDICT="$(STATE="$STATE" python <<'PY' 2>/dev/null
import os, sys, json
try:
    d = json.load(open(os.environ["STATE"]))
except Exception:
    sys.exit(0)  # estado ilegible -> no bloquear (auto-arme)
slice_ = d.get("active_slice")
if not slice_:
    sys.exit(0)  # sin slice: permitir declarar / planificar
code_phases = {"red", "green", "refactor", "smoke", "api", "data"}
if slice_.get("phase") not in code_phases:
    sys.exit(0)  # fases dor/change/pr/archived: permitido
ds = d.get("design_source") or {}
if ds.get("applies") is not True:
    sys.exit(0)  # proyecto sin UI (o indeterminado): mecanismo apagado
if (slice_.get("gates") or {}).get("fidelity") is not False:
    sys.exit(0)  # solo UI-pendiente (false) cuenta; null/ausente -> no bloquear
if ds.get("confirmed") is True:
    sys.exit(0)  # fuente de diseño confirmada: permitido
print("BLOCK")
PY
)"

if [ "$VERDICT" = "BLOCK" ]; then
  echo "⛔ design-source-guard: el slice con UI está en fase de código pero la fuente de diseño NO está confirmada." >&2
  echo "   Declara y confirma primero el DESIGN_SOURCE del proyecto (ver building-a-slice Fase 0-bis / DoR)." >&2
  echo "   El arnés NO genera el prototipo: declara la fuente (prototipo/export) y confírmala." >&2
  exit 2
fi
exit 0
