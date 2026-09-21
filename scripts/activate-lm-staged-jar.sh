#!/usr/bin/env bash
# Activate a Forge jar uploaded as *.jar.pending (server must be STOPPED).
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
RECYCLE="${LIVE_SFTP_RECYCLE:-recycle_bin}"

PENDING_NAME="${1:-}"
if [[ -z "$PENDING_NAME" ]]; then
  PENDING_NAME="$(basename "$(ls -1 "$ROOT"/mods/LegacyMechanics-*.jar 2>/dev/null | sort -V | tail -1)").pending"
fi

if [[ "${LIVE_SERVER_STOPPED:-}" != "STOPPED" ]]; then
  echo "Refusing to activate $PENDING_NAME — set LIVE_SERVER_STOPPED=STOPPED after a full panel stop." >&2
  exit 1
fi

if [[ -z "$HOST" || -z "$USER" || -z "$PASS" ]]; then
  echo "Live SFTP credentials missing (live-sftp.env)." >&2
  exit 1
fi

FINAL_NAME="${PENDING_NAME%.pending}"
export SSHPASS="$PASS"
SFTP_CMD=(sshpass -e sftp -o StrictHostKeyChecking=accept-new -P "$PORT")

echo "Activate $REMOTE_MODS/$PENDING_NAME -> $FINAL_NAME (recycle older LM jars)"

"${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $RECYCLE
rename $REMOTE_MODS/$FINAL_NAME $RECYCLE/$FINAL_NAME
rename $REMOTE_MODS/$PENDING_NAME $REMOTE_MODS/$FINAL_NAME
bye
EOF

echo "Activated. Start the server from the panel, then /lm admin reload if needed."
