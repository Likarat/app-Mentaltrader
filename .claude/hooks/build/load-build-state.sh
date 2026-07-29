#!/usr/bin/env bash
# load-build-state.sh — SessionStart
# Inyecta al contexto: rama actual, harness_phase, slice activo y gates abiertos.
# Además sincroniza harness_phase (authoring -> active) según exista package.json.
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib/state-io.sh"

# Lee el payload de SessionStart (JSON en stdin) para extraer session_id. Fail-open:
# si no hay stdin o no trae session_id, seguimos sin tocar session_continuity.
payload="$(cat 2>/dev/null || true)"

ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
STATE="$(state_path)"
BRANCH="$(git -C "$ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'desconocida')"

# Determina la fase real del arnés.
if [ -f "$ROOT/package.json" ]; then PHASE="active"; else PHASE="authoring"; fi

# Ancla el estado a disco antes de leerlo (fail-open; nunca rompe SessionStart).
if [ -f "$STATE" ] && command -v python >/dev/null 2>&1; then
  python "$(dirname "${BASH_SOURCE[0]}")/reconcile-build-state.py" "$STATE" 2>/dev/null || true
fi

# Reset once-per-SESSION del guard de auto-handoff (A1): si esta sesión (session_id del
# payload de SessionStart) es distinta de la última registrada en session_continuity,
# reabrimos critical_recorded para que context-monitor.sh pueda volver a escribir handoff
# fresco en ESTA sesión. Fail-open: sin stdin/session_id/state, no se toca nada.
if [ -f "$STATE" ] && command -v python >/dev/null 2>&1; then
  SESSION_ID="$(printf '%s' "$payload" | python -c '
import json,sys
try:
    p=json.load(sys.stdin)
    sid=p.get("session_id","")
    print(sid if isinstance(sid,str) else "")
except Exception:
    print("")
' 2>/dev/null || true)"
  if [ -n "$SESSION_ID" ]; then
    SID_JSON="$(python -c 'import json,sys; print(json.dumps(sys.argv[1]))' "$SESSION_ID" 2>/dev/null || true)"
    if [ -n "$SID_JSON" ]; then
      state_atomic_patch "$STATE" "
s = d.get('active_slice')
if isinstance(s, dict):
    sc = s.setdefault('session_continuity', {})
    if sc.get('last_session') != $SID_JSON:
        sc['last_session'] = $SID_JSON
        sc['critical_recorded'] = False
"
    fi
  fi
fi

# Sincroniza harness_phase en el estado (si python disponible y el archivo existe).
# Escritura ATÓMICA + validada: este hook corre en CADA SessionStart (alta frecuencia,
# headless incluido); una escritura no atómica que se interrumpa truncaría la ÚNICA
# fuente de verdad. Solo escribe si harness_phase cambia; si algo falla, deja el original
# intacto (fail-open) y nunca rompe el SessionStart.
if [ -f "$STATE" ] && command -v python >/dev/null 2>&1; then
  python - "$STATE" "$PHASE" <<'PY' 2>/dev/null || true
import json,sys,os,tempfile
path,phase=sys.argv[1],sys.argv[2]
try:
    with open(path) as fh:
        d=json.load(fh)
    # No escribir si no hay cambio.
    if d.get("harness_phase")==phase:
        sys.exit(0)
    # Validación mínima de forma antes de tocar disco (no relajamos el schema completo,
    # solo evitamos persistir algo que claramente no es un build-state).
    if not isinstance(d,dict) or not all(k in d for k in ("version","harness_phase","active_slice","history","releases")):
        sys.exit(0)
    d["harness_phase"]=phase
    dirn=os.path.dirname(path) or "."
    fd,tmp=tempfile.mkstemp(dir=dirn,prefix=".build-state.",suffix=".tmp")
    try:
        with os.fdopen(fd,"w") as out:
            json.dump(d,out,indent=2,ensure_ascii=False)
            out.flush()
            os.fsync(out.fileno())
        os.replace(tmp,path)  # sustitución atómica
    except Exception:
        try: os.unlink(tmp)
        except OSError: pass
        raise
except Exception:
    pass
PY
fi

echo "🏗️  Arnés de construcción"
echo "   Rama: $BRANCH | Fase del arnés: $PHASE"

if [ -f "$STATE" ] && command -v python >/dev/null 2>&1; then
  python - "$STATE" <<'PY' 2>/dev/null || true
import json,sys
d=json.load(open(sys.argv[1]))
s=d.get("active_slice")
if not s:
    print("   Slice activo: ninguno. Usa la skill building-a-slice para abrir uno (empieza por DoR).")
else:
    g=s.get("gates",{})
    abiertos=[k for k,v in g.items() if v is False]
    hus=", ".join(s.get("hus") or []) or "—"
    print(f"   Slice activo: {s.get('epica')} [{hus}] · change={s.get('openspec_change')} · fase={s.get('phase')}")
    print(f"   Gates pendientes: {', '.join(abiertos) if abiertos else 'ninguno ✅'}")
    # Handoff fino en disco (A1): items de cableado aún FAILING + última bitácora.
    # Una sesión fresca arranca de aquí; mientras queden failing, el cableado NO está hecho.
    failing=[w for w in (s.get("wiring_checklist") or []) if w.get("status")=="failing"]
    if failing:
        muestra="; ".join(f"{w.get('id')}({w.get('kind')})" for w in failing[:8])
        extra=f" (+{len(failing)-8} más)" if len(failing)>8 else ""
        print(f"   ⚠️  Cableado pendiente ({len(failing)} item/s failing): {muestra}{extra}")
        print("       NO declares el slice terminado mientras queden items failing (verifica con prueba real).")
    pend=[ss for ss in (s.get("sub_slices") or []) if ss.get("status")!="done"]
    if pend:
        print(f"   ⚠️  Sub-slices pendientes: {', '.join(ss.get('id') for ss in pend)}")
    log=s.get("progress_log") or []
    if log:
        last=log[-1]
        print(f"   Última bitácora: [{last.get('by')}] {last.get('note')}")
    sc=s.get("session_continuity") or {}
    if sc.get("resume_hint"):
        print(f"   ▶️  Retomar: {sc.get('resume_hint')}" + ("  [auto-continue]" if sc.get("auto_continue") else ""))
    if s.get("branch_drift"):
        print(f"   ⚠️  Rama real ({s.get('branch_drift')}) != branch del slice ({s.get('branch')}).")
PY
fi
exit 0
