#!/usr/bin/env bash
# Upload LegacyMechanics Forge + GUI jars to the test server (node.everal.net).
# Requires: sshpass (optional), TEST_SFTP_PASS or SSHPASS, or test-sftp.env in repo root.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${TEST_SFTP_HOST:-node.everal.net}"
PORT="${TEST_SFTP_PORT:-2022}"
USER="${TEST_SFTP_USER:-vamp.a6c38a00}"
REMOTE_MODS="${TEST_SFTP_MODS:-mods}"
REMOTE_PLUGINS="${TEST_SFTP_PLUGINS:-plugins}"
RECYCLE="${TEST_SFTP_RECYCLE:-recycle_bin}"

if [[ -f "$ROOT/test-sftp.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT/test-sftp.env"
fi
PASS="${TEST_SFTP_PASS:-${SSHPASS:-}}"
if [[ -z "$PASS" ]]; then
  echo "Set TEST_SFTP_PASS, SSHPASS, or create test-sftp.env (see docs/DEPLOY.md)." >&2
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

echo "Deploying to $USER@$HOST:$PORT"
echo "  $FORGE_NAME -> $REMOTE_MODS/"
echo "  $GUI_NAME -> $REMOTE_PLUGINS/"

"${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $RECYCLE
mkdir $REMOTE_MODS
mkdir $REMOTE_PLUGINS
cd $REMOTE_MODS
rename LegacyMechanics-*.jar $RECYCLE/ 2>/dev/null || true
put $FORGE_JAR $FORGE_NAME
cd ../$REMOTE_PLUGINS
rename LegacyMechanicsGUI-*.jar $RECYCLE/ 2>/dev/null || true
put $GUI_JAR $GUI_NAME
bye
EOF

echo "Done. Restart the test server from the panel, then run /lm admin reload in-game."
