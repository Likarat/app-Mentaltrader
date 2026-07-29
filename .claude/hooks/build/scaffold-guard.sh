#!/usr/bin/env bash
# scaffold-guard.sh — PreToolUse · Write/Edit/MultiEdit
# Backstop determinista del "Paso 1 fundamental": no se escribe código de slice sin
# scaffold confirmado. Enfoque A (por estado/fase): bloquea (exit 2) SOLO si hay un
# active_slice en fase de código (red/green/refactor/smoke/api/data) y scaffold.confirmed
# no es true. Permite crear el scaffold y planificar (sin slice, o fases dor/change).
# AUTO-ARME: si no existe build-state.json, no hay nada que vigilar -> exit 0.
set -uo pipefail

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
STATE="$ROOT/.claude/state/build-state.json"
[ -f "$STATE" ] || exit 0

INPUT="$(cat)"

# Guarda python [H4]: si falta, no podemos leer el estado de forma fiable. Fail-closed
# DIRIGIDO: solo bloqueamos si la edición NO es del propio scaffold/planificación obvia.
if ! command -v python >/dev/null 2>&1; then
  echo "⛔ scaffold-guard: python no disponible; no puedo verificar el gate de scaffold. Instala python (trycore-build doctor)." >&2
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
    sys.exit(0)  # sin slice: se permite crear el scaffold / planificar
code_phases = {"red", "green", "refactor", "smoke", "api", "data"}
if slice_.get("phase") not in code_phases:
    sys.exit(0)  # fases dor/change/pr/archived: permitido
if (d.get("scaffold") or {}).get("confirmed") is True:
    sys.exit(0)  # scaffold confirmado: permitido
print("BLOCK")
PY
)"

if [ "$VERDICT" = "BLOCK" ]; then
  echo "⛔ scaffold-guard (Paso 1 fundamental): el slice está en fase de código pero el scaffold NO está confirmado." >&2
  echo "   Confirma primero que existe un scaffold runnable del proyecto (ver building-a-slice Fase 0 / DoR)." >&2
  echo "   El arnés NO genera el scaffold: créalo según tu stack (stack-allowlist.json) y confírmalo." >&2
  exit 2
fi
exit 0
