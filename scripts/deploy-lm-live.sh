#!/usr/bin/env bash
# Upload LegacyMechanics Forge + GUI jars to LIVE production (explicit host only).
# Never defaults host/user — avoids accidental test deploy.
#
# Set via environment or repo-root live-sftp.env (gitignored):
#   LIVE_SFTP_HOST, LIVE_SFTP_PORT (default 2022), LIVE_SFTP_USER, LIVE_SFTP_PASS
# Optional: LIVE_SFTP_MODS, LIVE_SFTP_PLUGINS (default mods / plugins)
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
REMOTE_MODS="${LIVE_SFTP_MODS:-mods}"
REMOTE_PLUGINS="${LIVE_SFTP_PLUGINS:-plugins}"

if [[ -z "$HOST" || -z "$USER" || -z "$PASS" ]]; then
  echo "Live SFTP requires LIVE_SFTP_HOST, LIVE_SFTP_USER, and LIVE_SFTP_PASS" >&2
  echo "(or live-sftp.env in repo root — see docs/DEPLOY.md)." >&2
  exit 1
fi

FORGE_JAR="$(ls -1 "$ROOT"/mods/LegacyMechanics-*.jar 2>/dev/null | sort -V | tail -1)"
GUI_JAR="$(ls -1 "$ROOT"/plugins/LegacyMechanicsGUI-*.jar 2>/dev/null | sort -V | tail -1)"
if [[ ! -f "$FORGE_JAR" ]] || [[ ! -f "$GUI_JAR" ]]; then
  echo "Build jars first:" >&2
  echo "  bash tools/dmz-adaptive-difficulty/build.sh" >&2
  echo "  bash tools/dmz-adaptive-difficulty-gui/build.sh" >&2
  exit 1
fi

export SSHPASS="$PASS"
SFTP_CMD=(sftp -o StrictHostKeyChecking=accept-new -P "$PORT")
if command -v sshpass >/dev/null 2>&1; then
  SFTP_CMD=(sshpass -e sftp -o StrictHostKeyChecking=accept-new -P "$PORT")
fi

FORGE_NAME="$(basename "$FORGE_JAR")"
GUI_NAME="$(basename "$GUI_JAR")"

echo "LIVE deploy to $USER@$HOST:$PORT"
echo "  $FORGE_NAME -> $REMOTE_MODS/"
echo "  $GUI_NAME -> $REMOTE_PLUGINS/"
if [[ "${DEPLOY_LIVE_CONFIRM:-}" != "LIVE" ]]; then
  echo "Set DEPLOY_LIVE_CONFIRM=LIVE to skip interactive confirm (cloud agents)." >&2
  read -r -p "Type LIVE to confirm: " confirm
  if [[ "$confirm" != "LIVE" ]]; then
    echo "Aborted."
    exit 1
  fi
fi

"${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
put $FORGE_JAR $REMOTE_MODS/$FORGE_NAME
put $GUI_JAR $REMOTE_PLUGINS/$GUI_NAME
bye
EOF

echo "Upload complete. Restart the live server from the panel, then /lm admin reload."
