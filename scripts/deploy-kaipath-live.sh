#!/usr/bin/env bash
# Upload official/vendored KaiPath jar to LIVE mods/ and recycle older kaipath-*.jar copies.
# Usage: DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-kaipath-live.sh
# Optional: KAIPATH_DEPLOY_VERSION=1.3.7 (default: newest stock kaipath-*.jar, never *-lm-ultra)
# Official KaiPath feature work (textInput / field 11) is owned by the KaiPath mod developer —
# do not deploy overlay jars unless explicitly pinned.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ "${DEPLOY_LIVE_CONFIRM:-}" != "LIVE" ]]; then
  echo "Refusing: set DEPLOY_LIVE_CONFIRM=LIVE to upload KaiPath to production." >&2
  exit 1
fi

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
  echo "Live SFTP requires LIVE_SFTP_HOST, LIVE_SFTP_USER, and LIVE_SFTP_PASS" >&2
  exit 1
fi

pick_kaipath() {
  if [[ -n "${KAIPATH_DEPLOY_VERSION:-}" ]]; then
    local pinned="$ROOT/mods/kaipath-${KAIPATH_DEPLOY_VERSION}.jar"
    if [[ -f "$pinned" ]]; then
      echo "$pinned"
      return
    fi
    echo "KAIPATH_DEPLOY_VERSION=$KAIPATH_DEPLOY_VERSION but missing $pinned" >&2
    exit 1
  fi
  # Stock / official only — skip local *-lm-ultra overlay jars.
  local jar
  jar="$(ls -1 "$ROOT"/mods/kaipath-*.jar 2>/dev/null | grep -v -- '-lm-ultra\.jar$' | sort -V | tail -1 || true)"
  echo "${jar:-}"
}

JAR="$(pick_kaipath)"
if [[ -z "${JAR:-}" || ! -f "$JAR" ]]; then
  echo "No mods/kaipath-*.jar found — download KaiPath first." >&2
  exit 1
fi

export LIVE_SFTP_HOST="$HOST"
export LIVE_SFTP_PORT="$PORT"
export LIVE_SFTP_USER="$USER"
export LIVE_SFTP_PASS="$PASS"
export LIVE_SFTP_MODS="$REMOTE_MODS"
export LIVE_SFTP_RECYCLE="$RECYCLE"
export SSHPASS="$PASS"

echo "Deploying KaiPath $(basename "$JAR") -> $HOST:$REMOTE_MODS/"
python3 "$ROOT/scripts/kaipath-live-sftp-upload.py" --jar "$JAR"
echo "Done. Restart the live server to load the new KaiPath jar."
