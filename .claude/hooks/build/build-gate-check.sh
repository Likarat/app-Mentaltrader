#!/usr/bin/env bash
# build-gate-check.sh — Stop
# AUTO-ARME: inerte mientras no exista package.json.
# No bloquea: al cerrar el turno, avisa si hay un slice activo con gates abiertos.
set -uo pipefail

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
[ -f "$ROOT/package.json" ] || exit 0
STATE="$ROOT/.claude/state/build-state.json"
[ -f "$STATE" ] || exit 0
command -v python >/dev/null 2>&1 || exit 0

python - "$STATE" <<'PY' 2>/dev/null || true
import json,sys
d=json.load(open(sys.argv[1]))
s=d.get("active_slice")
if not s: sys.exit(0)
g=s.get("gates",{})
abiertos=[k for k,v in g.items() if v is False]
if abiertos:
    epica=s.get('epica') or '?'
    hus=', '.join(s.get('hus') or []) or '—'
    print(f"build-gate-check: slice {epica} [{hus}] en fase '{s.get('phase')}' con gates abiertos: {', '.join(abiertos)}.", file=sys.stderr)
    print("   No archives ni abras PR hasta cerrarlos (ver building-a-slice / dod.md).", file=sys.stderr)
PY
exit 0
