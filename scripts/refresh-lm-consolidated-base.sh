#!/usr/bin/env bash
# Copy the latest built LegacyMechanics jar into the consolidated base slot (local only; jar is gitignored).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/tools/dmz-adaptive-difficulty/base/LegacyMechanics-4.5.49-consolidated.jar"
VERSION="$(grep -oP 'public static final String VERSION = "\K[0-9.]+(?=")' \
  "$ROOT/tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java")"
SRC="$ROOT/mods/LegacyMechanics-${VERSION}.jar"
if [[ ! -f "$SRC" ]]; then
  echo "Missing $SRC — run tools/dmz-adaptive-difficulty/build.sh first" >&2
  exit 1
fi
mkdir -p "$(dirname "$DEST")"
cp -f "$SRC" "$DEST"
echo "Consolidated base updated: $DEST ($(du -h "$DEST" | cut -f1))"
