#!/usr/bin/env bash
# Upload dmz_mohist_melee_fix jar to live mods/ and recycle older melee fix jars.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
bash "$ROOT/scripts/ensure-live-sftp-env.sh" || true

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

if [[ -z "$HOST" || -z "$USER" || -z "$PASS" ]]; then
  echo "Live SFTP requires LIVE_SFTP_HOST, LIVE_SFTP_USER, LIVE_SFTP_PASS" >&2
  exit 1
fi

JAR="${MELEE_DEPLOY_JAR:-}"
if [[ -z "$JAR" ]]; then
  JAR="$(ls -1 "$ROOT/mods"/dmz_mohist_melee_fix-*.jar 2>/dev/null | sort -V | tail -1)"
fi
if [[ ! -f "$JAR" ]]; then
  echo "Missing melee fix jar under mods/ (build tools/dmz-mohist-melee-fix/build.sh first)." >&2
  exit 1
fi

JAR_NAME="$(basename "$JAR")"

if [[ "${DEPLOY_LIVE_CONFIRM:-}" != "LIVE" ]]; then
  echo "Refusing live upload without DEPLOY_LIVE_CONFIRM=LIVE" >&2
  echo "Would upload: $JAR_NAME -> $USER@$HOST:$REMOTE_MODS/" >&2
  exit 1
fi

export SSHPASS="$PASS"
SFTP_CMD=(sshpass -e sftp -o StrictHostKeyChecking=accept-new -P "$PORT")

list_melee() {
  printf 'ls -1 %s/dmz_mohist_melee_fix-*.jar\n' "$REMOTE_MODS" \
    | "${SFTP_CMD[@]}" "$USER@$HOST" 2>&1 \
    | rg -o 'dmz_mohist_melee_fix-[^[:space:]]+\.jar' || true
}

echo "Melee fix live deploy -> $USER@$HOST:$PORT"
echo "  $JAR -> $REMOTE_MODS/$JAR_NAME"

BATCH="$(mktemp)"
{
  echo "mkdir $REMOTE_MODS"
  echo "mkdir $RECYCLE"
  echo "put $JAR $REMOTE_MODS/$JAR_NAME"
  while IFS= read -r name; do
    [[ -z "$name" ]] && continue
    [[ "$name" == "$JAR_NAME" ]] && continue
    echo "rename $REMOTE_MODS/$name $RECYCLE/$name"
  done < <(list_melee)
  echo "bye"
} >"$BATCH"

"${SFTP_CMD[@]}" "$USER@$HOST" <"$BATCH"
rm -f "$BATCH"

echo "Uploaded $JAR_NAME; older dmz_mohist_melee_fix jars moved to $RECYCLE/."
echo "Restart the server so Forge loads the new melee fix mixins."
