#!/usr/bin/env bash
# Download live server mods/ (+ plugins/) for offline inspection (ProfTools, etc.).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/ensure-live-sftp-env.sh"
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
DEST="${1:-$ROOT/uploads/live-mods-$(date -u +%Y-%m-%d)}"
if [[ -z "$HOST" || -z "$USER" || -z "$PASS" ]]; then
  echo "Need LIVE_SFTP_HOST, LIVE_SFTP_USER, LIVE_SFTP_PASS (Cursor env secrets or live-sftp.env)." >&2
  exit 1
fi
mkdir -p "$DEST/mods" "$DEST/plugins"
export SSHPASS="$PASS"
SFTP=(sshpass -e sftp -o StrictHostKeyChecking=accept-new -P "$PORT")
list_remote() {
  local dir="$1"
  "${SFTP[@]}" "$USER@$HOST" <<EOF 2>/dev/null | rg '\.jar' | sed -E 's#.*/##' | rg '\.jar$' || true
ls $dir
bye
EOF
}
echo "Listing remote $REMOTE_MODS ..."
mapfile -t MOD_JARS < <(list_remote "$REMOTE_MODS")
echo "Found ${#MOD_JARS[@]} mod jar(s)."
for j in "${MOD_JARS[@]}"; do
  [[ -z "$j" ]] && continue
  echo "  get $REMOTE_MODS/$j"
  "${SFTP[@]}" "$USER@$HOST" <<EOF
get $REMOTE_MODS/$j $DEST/mods/$j
bye
EOF
done
mapfile -t PLUGIN_JARS < <(list_remote "$REMOTE_PLUGINS")
echo "Found ${#PLUGIN_JARS[@]} plugin jar(s)."
for j in "${PLUGIN_JARS[@]}"; do
  [[ -z "$j" ]] && continue
  echo "  get $REMOTE_PLUGINS/$j"
  "${SFTP[@]}" "$USER@$HOST" <<EOF
get $REMOTE_PLUGINS/$j $DEST/plugins/$j
bye
EOF
done
echo "Wrote: $DEST"
ls -1 "$DEST/mods" 2>/dev/null | sort -V
