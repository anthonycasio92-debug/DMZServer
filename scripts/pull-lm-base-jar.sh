#!/usr/bin/env bash
# Download the live Legacy Mechanics Forge base jar (ki/stamina overflow fix line).
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
REMOTE="${LM_BASE_REMOTE:-mods/LegacyMechanics-4.5.23-direct-dmz-resource-max.jar}"
NAME="$(basename "$REMOTE")"
DEST="$ROOT/tools/dmz-adaptive-difficulty/base/$NAME"
MODS="$ROOT/mods/$NAME"
if [[ -z "$HOST" || -z "$USER" || -z "$PASS" ]]; then
  echo "Need LIVE_SFTP_* or live-sftp.env" >&2
  exit 1
fi
mkdir -p "$(dirname "$DEST")" "$ROOT/mods"
export SSHPASS="$PASS"
SFTP=(sshpass -e sftp -o StrictHostKeyChecking=accept-new -P "$PORT")
"${SFTP[@]}" "$USER@$HOST" <<EOF
get $REMOTE $DEST
EOF
cp -f "$DEST" "$MODS"
echo "Base jar: $DEST"
echo "Copy:     $MODS"
