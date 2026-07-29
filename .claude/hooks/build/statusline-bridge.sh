#!/usr/bin/env bash
# statusline-bridge.sh — statusLine command (canal CLI). Imprime la línea de estado
# y escribe el puente de contexto que lee context-monitor.sh. Fail-open siempre.
set -uo pipefail
payload="$(cat)"
command -v python >/dev/null 2>&1 || { echo "🏗️ build"; exit 0; }
echo "$payload" | python -c '
import json,sys,os,time,re,tempfile
try:
    p=json.load(sys.stdin)
except Exception:
    print("🏗️ build"); sys.exit(0)
sid=str(p.get("session_id","default"))
if re.search(r"[\\/]|\.\.", sid): sid=re.sub(r"[^A-Za-z0-9_-]","_",sid)  # sanitiza path traversal
rem=None
try: rem=int(p.get("context_window",{}).get("remaining_percentage"))
except Exception: rem=None
if rem is not None:
    d={"remaining_pct":rem,"used_pct":100-rem,"ts":int(time.time())}
    path=os.path.join(tempfile.gettempdir(), f"claude-ctx-{sid}.json")
    try:
        fd,tmp=tempfile.mkstemp(dir=tempfile.gettempdir(),prefix=".ctx-",suffix=".tmp")
        try:
            with os.fdopen(fd,"w") as f: json.dump(d,f)
            os.replace(tmp,path)
        except Exception:
            try: os.unlink(tmp)
            except OSError: pass
    except Exception: pass
tag = f"🏗️ build · ctx {rem}%" if rem is not None else "🏗️ build"
print(tag)
' 2>/dev/null || echo "🏗️ build"
