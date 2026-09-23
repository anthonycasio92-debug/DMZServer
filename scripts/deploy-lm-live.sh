#!/usr/bin/env bash
# Upload LegacyMechanics Forge + GUI jars to LIVE production (explicit host only).
# Default: write active jar names over SFTP while the server is running (owner policy).
# Optional: LM_STAGE_PENDING=1 uploads *.jar.pending instead (no overwrite until activate).
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

SKIP_GUI="${LM_SKIP_GUI:-0}"
FORGE_JAR="$(pick_lm_jar "$ROOT/mods" LegacyMechanics)"
GUI_JAR=""
if [[ "$SKIP_GUI" != "1" ]]; then
  GUI_JAR="$(pick_lm_jar "$ROOT/plugins" LegacyMechanicsGUI)"
fi
if [[ ! -f "$FORGE_JAR" ]]; then
  echo "Build Forge jar first: bash tools/dmz-adaptive-difficulty/build.sh" >&2
  exit 1
fi
if [[ "$SKIP_GUI" != "1" && ! -f "$GUI_JAR" ]]; then
  echo "Build GUI jar or set LM_SKIP_GUI=1 for Forge-only deploy:" >&2
  echo "  bash tools/dmz-adaptive-difficulty-gui/build.sh" >&2
  exit 1
fi

export SSHPASS="$PASS"
SFTP_CMD=(sftp -o StrictHostKeyChecking=accept-new -P "$PORT")
if command -v sshpass >/dev/null 2>&1; then
  SFTP_CMD=(sshpass -e sftp -o StrictHostKeyChecking=accept-new -P "$PORT")
fi

FORGE_NAME="$(basename "$FORGE_JAR")"
GUI_NAME=""
if [[ -n "$GUI_JAR" ]]; then
  GUI_NAME="$(basename "$GUI_JAR")"
fi
STAGE_PENDING="${LM_STAGE_PENDING:-0}"

recycle_remote_lm_jars() {
  local list_file
  list_file="$(mktemp)"
  if [[ -n "$GUI_NAME" ]]; then
    printf 'ls -1 %s/LegacyMechanics-*.jar\nls -1 %s/LegacyMechanicsGUI-*.jar\n' "$REMOTE_MODS" "$REMOTE_PLUGINS" \
      | "${SFTP_CMD[@]}" "$USER@$HOST" 2>/dev/null | rg -o 'LegacyMechanics[^[:space:]]+\.jar(\.pending)?' | sort -u >"$list_file" || true
  else
    printf 'ls -1 %s/LegacyMechanics-*.jar\n' "$REMOTE_MODS" \
      | "${SFTP_CMD[@]}" "$USER@$HOST" 2>/dev/null | rg -o 'LegacyMechanics[^[:space:]]+\.jar(\.pending)?' | sort -u >"$list_file" || true
  fi
  local cmds=""
  while IFS= read -r name; do
    [[ -z "$name" ]] && continue
    if [[ "$name" == "$FORGE_NAME" || "$name" == "$GUI_NAME" ]]; then
      continue
    fi
    if [[ "$name" == "${FORGE_NAME}.pending" || "$name" == "${GUI_NAME}.pending" ]]; then
      continue
    fi
    case "$name" in
      LegacyMechanics-*.jar|LegacyMechanics-*.jar.pending)
        cmds+="rename $REMOTE_MODS/$name $RECYCLE/$name"$'\n'
        ;;
      LegacyMechanicsGUI-*.jar|LegacyMechanicsGUI-*.jar.pending)
        cmds+="rename $REMOTE_PLUGINS/$name $RECYCLE/$name"$'\n'
        ;;
    esac
  done <"$list_file"
  rm -f "$list_file"
  if [[ -n "$cmds" ]]; then
    printf 'mkdir %s\n%s' "$RECYCLE" "$cmds" | "${SFTP_CMD[@]}" "$USER@$HOST" || true
  fi
}

echo "LIVE deploy to $USER@$HOST:$PORT"
if [[ "$STAGE_PENDING" == "1" ]]; then
  echo "  $FORGE_NAME -> $REMOTE_MODS/${FORGE_NAME}.pending (LM_STAGE_PENDING=1)"
  echo "  $GUI_NAME -> $REMOTE_PLUGINS/${GUI_NAME}.pending"
  echo "  Activate after stop: LIVE_SERVER_STOPPED=STOPPED bash scripts/activate-lm-staged-jar.sh"
else
  echo "  $FORGE_NAME -> $REMOTE_MODS/ (direct — server may stay up)"
  if [[ -n "$GUI_NAME" ]]; then
    echo "  $GUI_NAME -> $REMOTE_PLUGINS/"
  else
    echo "  (LM_SKIP_GUI=1 — Forge only; recycle LegacyMechanicsGUI on live after restart)"
  fi
  echo "  Restart (or /lm admin reload for config) to pick up Forge/GUI changes."
fi

if [[ "${DEPLOY_LIVE_CONFIRM:-}" != "LIVE" ]]; then
  echo "Set DEPLOY_LIVE_CONFIRM=LIVE to skip interactive confirm (cloud agents)." >&2
  read -r -p "Type LIVE to confirm: " confirm
  if [[ "$confirm" != "LIVE" ]]; then
    echo "Aborted."
    exit 1
  fi
fi

# Owner manifest — block live upload if agreed behaviors are missing from this tree.
MANIFEST_AUDIT="$ROOT/tools/dmz-adaptive-difficulty/sim/audit_ship_manifest.py"
if [[ -f "$MANIFEST_AUDIT" ]]; then
  echo "Running ship manifest audit before upload..."
  if ! python3 "$MANIFEST_AUDIT"; then
    echo "DEPLOY ABORTED: audit_ship_manifest.py failed (see docs/LM_SHIP_MANIFEST.md)." >&2
    exit 1
  fi
else
  echo "WARN: missing audit_ship_manifest.py — deploy not gated" >&2
fi

# Version in jars must match source pin when LM_DEPLOY_VERSION is set.
FORGE_VER="$(unzip -p "$FORGE_JAR" META-INF/mods.toml 2>/dev/null | rg '^version\s*=' | head -1 | rg -o '[0-9]+\.[0-9]+\.[0-9]+' || true)"
GUI_VER=""
if [[ -n "$GUI_JAR" ]]; then
  GUI_VER="$(unzip -p "$GUI_JAR" plugin.yml 2>/dev/null | rg '^version:' | head -1 | rg -o '[0-9]+\.[0-9]+\.[0-9]+' || true)"
fi
if [[ -n "${LM_DEPLOY_VERSION:-}" && -n "$FORGE_VER" && "$FORGE_VER" != "$LM_DEPLOY_VERSION" ]]; then
  echo "DEPLOY ABORTED: Forge jar version $FORGE_VER != LM_DEPLOY_VERSION=$LM_DEPLOY_VERSION" >&2
  exit 1
fi
if [[ -n "$FORGE_VER" && -n "$GUI_VER" && "$FORGE_VER" != "$GUI_VER" ]]; then
  echo "DEPLOY ABORTED: Forge $FORGE_VER != GUI $GUI_VER" >&2
  exit 1
fi

recycle_remote_gui_jars() {
  if [[ "$SKIP_GUI" != "1" ]]; then
    return
  fi
  local list_file
  list_file="$(mktemp)"
  printf 'ls -1 %s/LegacyMechanicsGUI-*.jar\n' "$REMOTE_PLUGINS" \
    | "${SFTP_CMD[@]}" "$USER@$HOST" 2>/dev/null | rg -o 'LegacyMechanicsGUI[^[:space:]]+\.jar(\.pending)?' | sort -u >"$list_file" || true
  local cmds=""
  while IFS= read -r name; do
    [[ -z "$name" ]] && continue
    cmds+="rename $REMOTE_PLUGINS/$name $RECYCLE/$name"$'\n'
  done <"$list_file"
  rm -f "$list_file"
  if [[ -n "$cmds" ]]; then
    printf 'mkdir %s\n%s' "$RECYCLE" "$cmds" | "${SFTP_CMD[@]}" "$USER@$HOST" || true
  fi
}

if [[ "$STAGE_PENDING" == "1" ]]; then
  if [[ -n "$GUI_JAR" ]]; then
    "${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $REMOTE_MODS
mkdir $REMOTE_PLUGINS
put $FORGE_JAR $REMOTE_MODS/${FORGE_NAME}.pending
put $GUI_JAR $REMOTE_PLUGINS/${GUI_NAME}.pending
bye
EOF
  else
    "${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $REMOTE_MODS
put $FORGE_JAR $REMOTE_MODS/${FORGE_NAME}.pending
bye
EOF
  fi
  echo "Staged pending jars. Active files unchanged until activate-lm-staged-jar.sh"
else
  if [[ -n "$GUI_JAR" ]]; then
    "${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $RECYCLE
mkdir $REMOTE_MODS
mkdir $REMOTE_PLUGINS
put $FORGE_JAR $REMOTE_MODS/$FORGE_NAME
put $GUI_JAR $REMOTE_PLUGINS/$GUI_NAME
bye
EOF
  else
    "${SFTP_CMD[@]}" "$USER@$HOST" <<EOF
mkdir $RECYCLE
mkdir $REMOTE_MODS
put $FORGE_JAR $REMOTE_MODS/$FORGE_NAME
bye
EOF
    recycle_remote_gui_jars
  fi
  recycle_remote_lm_jars
  echo "Upload complete."
fi
