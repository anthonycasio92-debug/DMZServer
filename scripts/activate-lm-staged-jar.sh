#!/usr/bin/env bash
# Activate Forge/GUI jars uploaded as *.jar.pending (server must be STOPPED).
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
RECYCLE="${LIVE_SFTP_RECYCLE:-recycle_bin}"

FORGE_PENDING="${1:-}"
if [[ -z "$FORGE_PENDING" ]]; then
  FORGE_PENDING="$(basename "$(ls -1 "$ROOT"/mods/LegacyMechanics-*.jar 2>/dev/null | sort -V | tail -1)").pending"
fi
GUI_PENDING="${2:-}"
if [[ -z "$GUI_PENDING" ]]; then
  GUI_PENDING="$(basename "$(ls -1 "$ROOT"/plugins/LegacyMechanicsGUI-*.jar 2>/dev/null | sort -V | tail -1)").pending"
fi

if [[ "${LIVE_SERVER_STOPPED:-}" != "STOPPED" ]]; then
  echo "Refusing to activate pending jars — set LIVE_SERVER_STOPPED=STOPPED after a full panel stop." >&2
  exit 1
fi

if [[ -z "$HOST" || -z "$USER" || -z "$PASS" ]]; then
  echo "Live SFTP credentials missing (live-sftp.env)." >&2
  exit 1
fi

FORGE_FINAL="${FORGE_PENDING%.pending}"
GUI_FINAL="${GUI_PENDING%.pending}"

export SSHPASS="$PASS"
SFTP_CMD=(sshpass -e sftp -o StrictHostKeyChecking=accept-new -P "$PORT")

echo "Activate pending LM jars on live"
echo "  Forge: $REMOTE_MODS/$FORGE_PENDING -> $FORGE_FINAL"
echo "  GUI:   $REMOTE_PLUGINS/$GUI_PENDING -> $GUI_FINAL"

"${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $RECYCLE
rename $REMOTE_MODS/$FORGE_FINAL $RECYCLE/$FORGE_FINAL
rename $REMOTE_MODS/$FORGE_PENDING $REMOTE_MODS/$FORGE_FINAL
rename $REMOTE_PLUGINS/$GUI_FINAL $RECYCLE/$GUI_FINAL
rename $REMOTE_PLUGINS/$GUI_PENDING $REMOTE_PLUGINS/$GUI_FINAL
bye
EOF

echo "Activated. Start the server from the panel, then /lm admin reload if needed."
