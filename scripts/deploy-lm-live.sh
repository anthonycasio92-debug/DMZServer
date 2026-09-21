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
RECYCLE="${LIVE_SFTP_RECYCLE:-recycle_bin}"

if [[ -z "$HOST" || -z "$USER" || -z "$PASS" ]]; then
  echo "Live SFTP requires LIVE_SFTP_HOST, LIVE_SFTP_USER, and LIVE_SFTP_PASS" >&2
  echo "(or live-sftp.env in repo root — see docs/DEPLOY.md)." >&2
  exit 1
fi

pick_lm_jar() {
  local dir="$1" prefix="$2"
  if [[ -n "${LM_DEPLOY_VERSION:-}" ]]; then
    local pinned="$dir/${prefix}-${LM_DEPLOY_VERSION}.jar"
    if [[ -f "$pinned" ]]; then
      echo "$pinned"
      return
    fi
    echo "LM_DEPLOY_VERSION=$LM_DEPLOY_VERSION but missing $pinned" >&2
    exit 1
  fi
  ls -1 "$dir"/${prefix}-*.jar 2>/dev/null | sort -V | tail -1
}

FORGE_JAR="$(pick_lm_jar "$ROOT/mods" LegacyMechanics)"
GUI_JAR="$(pick_lm_jar "$ROOT/plugins" LegacyMechanicsGUI)"
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

recycle_remote_lm_jars() {
  local list_file
  list_file="$(mktemp)"
  printf 'ls -1 %s/LegacyMechanics-*.jar\nls -1 %s/LegacyMechanicsGUI-*.jar\n' "$REMOTE_MODS" "$REMOTE_PLUGINS" \
    | "${SFTP_CMD[@]}" "$USER@$HOST" 2>/dev/null | rg -o 'LegacyMechanics[^[:space:]]+\.jar' | sort -u >"$list_file" || true
  local cmds=""
  while IFS= read -r name; do
    [[ -z "$name" ]] && continue
    if [[ "$name" == "$FORGE_NAME" || "$name" == "$GUI_NAME" ]]; then
      continue
    fi
    if [[ "$name" == LegacyMechanics-*.jar ]]; then
      cmds+="rename $REMOTE_MODS/$name $RECYCLE/$name"$'\n'
    elif [[ "$name" == LegacyMechanicsGUI-*.jar ]]; then
      cmds+="rename $REMOTE_PLUGINS/$name $RECYCLE/$name"$'\n'
    fi
  done <"$list_file"
  rm -f "$list_file"
  if [[ -n "$cmds" ]]; then
    printf 'mkdir %s\n%s' "$RECYCLE" "$cmds" | "${SFTP_CMD[@]}" "$USER@$HOST"
  fi
}

FORGE_PENDING="${FORGE_NAME}.pending"
GUI_PENDING="${GUI_NAME}.pending"
STOPPED="${LIVE_SERVER_STOPPED:-}"

echo "LIVE deploy to $USER@$HOST:$PORT"
if [[ "$STOPPED" == "STOPPED" ]]; then
  echo "  $FORGE_NAME -> $REMOTE_MODS/ (server STOPPED — direct install)"
  echo "  $GUI_NAME -> $REMOTE_PLUGINS/"
else
  echo "  $FORGE_NAME -> $REMOTE_MODS/$FORGE_PENDING (STAGED — server still running)"
  echo "  $GUI_NAME -> $REMOTE_PLUGINS/ (GUI only if STOPPED; else skip Forge swap)"
  echo ""
  echo "Never overwrite LegacyMechanics-*.jar while Mohist is running (invalid playerdata)." >&2
  echo "Stop the panel, then: LIVE_SERVER_STOPPED=STOPPED DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-live.sh" >&2
  echo "Or: LIVE_SERVER_STOPPED=STOPPED bash scripts/activate-lm-staged-jar.sh $FORGE_PENDING" >&2
fi

if [[ "${DEPLOY_LIVE_CONFIRM:-}" != "LIVE" ]]; then
  echo "Set DEPLOY_LIVE_CONFIRM=LIVE to skip interactive confirm (cloud agents)." >&2
  read -r -p "Type LIVE to confirm: " confirm
  if [[ "$confirm" != "LIVE" ]]; then
    echo "Aborted."
    exit 1
  fi
fi

if [[ "$STOPPED" == "STOPPED" ]]; then
  "${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $RECYCLE
mkdir $REMOTE_MODS
mkdir $REMOTE_PLUGINS
put $FORGE_JAR $REMOTE_MODS/$FORGE_NAME
put $GUI_JAR $REMOTE_PLUGINS/$GUI_NAME
bye
EOF
  recycle_remote_lm_jars
  echo "Upload complete. Start the server from the panel, then /lm admin reload."
else
  "${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $REMOTE_MODS
put $FORGE_JAR $REMOTE_MODS/$FORGE_PENDING
bye
EOF
  echo "Staged $FORGE_PENDING only. GUI jar not replaced while server is up."
  echo "After panel STOP: LIVE_SERVER_STOPPED=STOPPED bash scripts/activate-lm-staged-jar.sh $FORGE_PENDING"
fi
