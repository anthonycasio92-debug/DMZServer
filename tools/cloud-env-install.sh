#!/usr/bin/env bash
# Cloud-agent bootstrap for DMZServer (script editing environment).
# Idempotent, terminates quickly. Does NOT start Minecraft.
set -euo pipefail

cd /workspace

echo "[dmz-env] Java: $(java -version 2>&1 | head -1)"
echo "[dmz-env] Node: $(node -v)"
echo "[dmz-env] Python: $(python3 --version)"

missing=0
for p in \
  kubejs/server_scripts \
  kubejs/startup_scripts \
  kubejs/client_scripts \
  customnpcs/scripts \
  customnpcs/scripts/player_scripts.json \
  forge_scripts.json \
  tools/SCRIPT_LAYOUT.md
do
  if [ ! -e "$p" ]; then
    echo "[dmz-env] MISSING: $p"
    missing=1
  fi
done

# Conflict guards: superseded scripts must not be in live paths
for bad in \
  "customnpcs/scripts/Fly.js" \
  "customnpcs/scripts/JumpSprint.js" \
  "customnpcs/scripts/flight suppression.js" \
  "customnpcs/scripts/DMZ Energy.js" \
  "customnpcs/scripts/EndDragon-Forge-Trigger.js"
do
  if [ -f "$bad" ]; then
    echo "[dmz-env] CONFLICT still live: $bad"
    missing=1
  fi
done

# forge scripts must stay disabled in the repo template
if ! grep -q '"ScriptEnabled": 0b' forge_scripts.json; then
  echo "[dmz-env] WARN: forge_scripts.json should have ScriptEnabled 0b"
fi

kube_n=$(find kubejs -name '*.js' ! -path '*/examples/*' | wc -l | tr -d ' ')
cnpc_n=$(find customnpcs/scripts -maxdepth 1 -name '*.js' | wc -l | tr -d ' ')
echo "[dmz-env] kubejs .js files: $kube_n"
echo "[dmz-env] cnpc live .js files: $cnpc_n"

if [ "$missing" -ne 0 ]; then
  echo "[dmz-env] bootstrap FAILED"
  exit 1
fi

echo "[dmz-env] bootstrap OK (secrets not required)"
