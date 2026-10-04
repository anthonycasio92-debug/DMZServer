#!/usr/bin/env bash
# Materialize gitignored live-sftp.env from Cursor/agent secrets (LIVE_SFTP_*).
# Never logs or prints credentials.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT/live-sftp.env"

HOST="${LIVE_SFTP_HOST:-}"
PORT="${LIVE_SFTP_PORT:-2022}"
USER="${LIVE_SFTP_USER:-}"
PASS="${LIVE_SFTP_PASS:-}"

have_secrets=0
if [[ -n "$HOST" && -n "$USER" && -n "$PASS" ]]; then
  have_secrets=1
fi

if [[ -f "$ENV_FILE" && "$have_secrets" -eq 0 ]]; then
  exit 0
fi

if [[ "$have_secrets" -eq 0 ]]; then
  exit 0
fi

umask 077
cat >"$ENV_FILE" <<EOF
# Generated from agent secrets — do not commit.
LIVE_SFTP_HOST=${HOST}
LIVE_SFTP_PORT=${PORT}
LIVE_SFTP_USER=${USER}
LIVE_SFTP_PASS='${PASS//\'/\'\\\'\'}'
EOF
