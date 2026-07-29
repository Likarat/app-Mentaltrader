#!/usr/bin/env bash
# gitflow-guard.sh — PreToolUse · Bash
# Enforce GitHub Flow estricto:
#   - Prohíbe `git commit` directo en main/master.
#   - Prohíbe `git push` que apunte a main/master (la integración va por PR).
#   - Exige trabajar en rama feature/* | fix/* | chore/* para commitear.
# Bloquea con exit 2 (stderr se devuelve a Claude). Cualquier otra cosa -> exit 0.
set -euo pipefail

INPUT="$(cat)"

# Guarda python [H4]: si falta, no podemos parsear el JSON del hook. Fail-closed DIRIGIDO:
# solo bloqueamos (exit 2) si el comando crudo parece un git commit/push; si no, exit 0.
if ! command -v python >/dev/null 2>&1; then
  if printf '%s' "$INPUT" | grep -Eq 'git[^"]*(commit|push)'; then
    echo "⛔ gitflow-guard: python no disponible; no puedo analizar el comando git. Instala python (trycore-build doctor)." >&2
    exit 2
  fi
  exit 0
fi

CMD="$(printf '%s' "$INPUT" | python -c 'import sys,json
try:
    print(json.load(sys.stdin).get("tool_input",{}).get("command",""))
except Exception:
    print("")')"

# Sólo nos interesan comandos git de escritura.
if ! printf '%s' "$CMD" | grep -Eq '(^|[;&| ])git[ ]'; then
  exit 0
fi

is_commit=false
is_push=false
printf '%s' "$CMD" | grep -Eq '(^|[;&| ])git[ ]+(.*[ ])?commit([ ]|$)' && is_commit=true
printf '%s' "$CMD" | grep -Eq '(^|[;&| ])git[ ]+(.*[ ])?push([ ]|$)'   && is_push=true

if [ "$is_commit" = false ] && [ "$is_push" = false ]; then
  exit 0
fi

BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '')"

block() {
  echo "⛔ gitflow-guard (GitHub Flow estricto): $1" >&2
  echo "   Política: main protegida · trabajar en feature/* | fix/* | chore/* · integrar por PR." >&2
  exit 2
}

# 1) Commit directo a main/master.
if [ "$is_commit" = true ] && printf '%s' "$BRANCH" | grep -Eq '^(main|master)$'; then
  block "commit directo a '$BRANCH' no permitido. Crea una rama: git switch -c feature/<slug>"
fi

# 2) Commit desde una rama no tipada.
if [ "$is_commit" = true ] && [ -n "$BRANCH" ] && ! printf '%s' "$BRANCH" | grep -Eq '^(feature|fix|chore)/'; then
  block "rama '$BRANCH' no sigue la convención. Usa feature/* | fix/* | chore/* para commitear."
fi

# 3) Push apuntando a main/master.
if [ "$is_push" = true ] && printf '%s' "$CMD" | grep -Eq 'push[^|;&]*[ :](main|master)([ ]|$|:)'; then
  block "push directo a main/master no permitido. Abre un Pull Request desde tu rama feature/*."
fi

exit 0
