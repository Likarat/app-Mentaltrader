#!/usr/bin/env bash
# context-monitor.sh — PostToolUse|PreCompact|Stop. Lee el puente, aplica umbrales,
# inyecta additionalContext y (en critical) escribe handoff. Fail-open.
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HERE/lib/state-io.sh"
payload="$(cat)"; command -v python >/dev/null 2>&1 || exit 0

read -r SID EVT <<<"$(printf '%s' "$payload" | python -c '
import json,sys
try: p=json.load(sys.stdin)
except Exception: p={}
print(p.get("session_id","default"), p.get("hook_event_name","PostToolUse"))
' 2>/dev/null)"
SID="${SID:-default}"; EVT="${EVT:-PostToolUse}"

sanitized_sid="$(printf '%s' "$SID" | tr -c 'A-Za-z0-9_-' '_')"
BRIDGE="${TMPDIR:-/tmp}/claude-ctx-$sanitized_sid.json"
[ -f "$BRIDGE" ] || BRIDGE="$(command ls "${TMPDIR:-/tmp}"/claude-ctx-*.json 2>/dev/null | head -1)"
[ -n "${BRIDGE:-}" ] && [ -f "$BRIDGE" ] || exit 0

WARN="$(config_get context.warning_pct 35)"; CRIT="$(config_get context.critical_pct 25)"
STALE="$(config_get context.stale_seconds 60)"; AUTO="$(config_get context.auto_checkpoint false)"
STATE="$(state_path)"

python - "$BRIDGE" "$WARN" "$CRIT" "$STALE" "$EVT" "$STATE" "$AUTO" <<'PY' 2>/dev/null || true
import json,sys,os,time,tempfile
bridge,warn,crit,stale,evt,state,auto=sys.argv[1:8]
warn,crit,stale=int(warn),int(crit),int(stale)
try: b=json.load(open(bridge))
except Exception: sys.exit(0)
if time.time()-b.get("ts",0) > stale: sys.exit(0)   # métrica vieja
rem=b.get("remaining_pct",100)
sev = "critical" if rem<=crit else ("warning" if rem<=warn else None)
if not sev: sys.exit(0)

# ¿slice activo? handoff solo en critical y una vez por sesión.
recorded=False
if sev=="critical" and os.path.exists(state):
    try:
        d=json.load(open(state)); s=d.get("active_slice")
        if s:
            sc=s.setdefault("session_continuity",{})
            if not sc.get("critical_recorded"):
                iso=time.strftime("%Y-%m-%dT%H:%M:%SZ",time.gmtime())
                sc["stopped_at"]=f"context exhaustion at {rem}% ({iso})"
                failing=[w["id"] for w in (s.get("wiring_checklist") or []) if w.get("status")=="failing"]
                sc["resume_hint"]=("cablear: "+", ".join(failing[:6])) if failing else "revisar progreso y continuar"
                sc["critical_recorded"]=True
                sc["auto_continue"]=(auto=="true")
                s.setdefault("progress_log",[]).append(
                    {"at":iso,"by":"context-monitor","note":f"handoff auto a {rem}% de contexto restante"})
                dirn=os.path.dirname(state) or "."
                fd,tmp=tempfile.mkstemp(dir=dirn,prefix=".build-state.",suffix=".tmp")
                try:
                    with os.fdopen(fd,"w") as o: json.dump(d,o,indent=2,ensure_ascii=False); o.flush(); os.fsync(o.fileno())
                    os.replace(tmp,state); recorded=True
                except Exception:
                    try: os.unlink(tmp)
                    except OSError: pass
                    raise
    except Exception: pass

if sev=="warning":
    msg=(f"⚠️ Contexto al {rem}% restante. Acércate a un punto natural de corte "
         "(fin de fase/gate). No inicies trabajo complejo nuevo.")
else:
    tail=(" Handoff escrito en build-state.json (session_continuity)." if recorded else "")
    cont=(" auto_checkpoint=ON: continúa en sesión fresca." if auto=="true" else
          " Avisa al usuario para reiniciar en un punto natural.")
    msg=(f"🛑 Contexto CRÍTICO al {rem}% restante.{tail} El estado ya vive en build-state.json;"
         f" no reescribas handoff manual.{cont}")
# En 'Stop', inyectar additionalContext RE-LANZA el turno (re-prompt): repetirlo en cada
# intento de cierre entra en bucle hasta el tope CLAUDE_CODE_STOP_HOOK_BLOCK_CAP (=9→override).
# Por eso en 'Stop' re-lanzamos como MUCHO una vez por sesión y solo en la TRANSICIÓN a crítico
# (recorded=True, el instante en que se graba el handoff). 'warning' nunca inyecta en 'Stop'
# (el nudge solo sirve mientras se trabaja). Fuera de 'Stop' se inyecta con normalidad.
if evt=="Stop" and not (sev=="critical" and recorded): sys.exit(0)
print(json.dumps({"hookSpecificOutput":{"hookEventName":evt,"additionalContext":msg}}))
PY
