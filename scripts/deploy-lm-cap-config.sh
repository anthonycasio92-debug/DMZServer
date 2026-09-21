#!/usr/bin/env bash
# Push Overhaul level-cap JSON + remove obsolete KubeJS cap shims on LIVE.
# Safe while the server is running (config reload / next join); does not touch Forge jars.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -f "$ROOT/live-sftp.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/live-sftp.env"
fi

HOST="${LIVE_SFTP_HOST:-}"
PORT="${LIVE_SFTP_PORT:-2022}"
USER="${LIVE_SFTP_USER:-}"
PASS="${LIVE_SFTP_PASS:-${SSHPASS:-}}"
REMOTE_CONFIG="${LIVE_SFTP_CONFIG:-config}"

if [[ -z "$HOST" || -z "$USER" || -z "$PASS" ]]; then
  echo "Live SFTP requires LIVE_SFTP_HOST, LIVE_SFTP_USER, and LIVE_SFTP_PASS" >&2
  exit 1
fi

JSON="$ROOT/config/dmzrevamp/LevelingRevamp.json"
if [[ ! -f "$JSON" ]]; then
  echo "Missing $JSON" >&2
  exit 1
fi

export SSHPASS="$PASS"
SFTP_CMD=(sshpass -e sftp -o StrictHostKeyChecking=accept-new -P "$PORT")

echo "LIVE cap config -> $USER@$HOST:$PORT"
echo "  $JSON -> $REMOTE_CONFIG/dmzrevamp/LevelingRevamp.json"
echo "  remove kubejs/**/overhaul_level_cap.js (if present)"

if [[ "${DEPLOY_LIVE_CONFIRM:-}" != "LIVE" ]]; then
  echo "Set DEPLOY_LIVE_CONFIRM=LIVE to confirm." >&2
  exit 1
fi

KUBEJS_RM=""
for path in \
  kubejs/server_scripts/overhaul_level_cap.js \
  kubejs/client_scripts/overhaul_level_cap.js \
  kubejs/startup_scripts/overhaul_level_cap.js; do
  KUBEJS_RM+="rm $path"$'\n'
done

"${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $REMOTE_CONFIG
mkdir $REMOTE_CONFIG/dmzrevamp
put $JSON $REMOTE_CONFIG/dmzrevamp/LevelingRevamp.json
$KUBEJS_RM
bye
EOF

echo "Config uploaded. Restart server or reload Overhaul config if your pack supports it."
