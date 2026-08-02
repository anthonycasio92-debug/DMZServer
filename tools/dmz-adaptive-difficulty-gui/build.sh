#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
HERE="$(cd "$(dirname "$0")" && pwd)"
PAPER="$ROOT/libraries/paper-api-1.20.1.jar"
PAPI="$ROOT/plugins/PlaceholderAPI-2.12.3.jar"
CMILIB="$ROOT/plugins/CMILib1.5.9.6.jar"
ADV_API="$ROOT/libraries/adventure-api-4.14.0.jar"
ADV_KEY="$ROOT/libraries/adventure-key-4.14.0.jar"
EXAM_API="$ROOT/libraries/examination-api-1.3.0.jar"
EXAM_STR="$ROOT/libraries/examination-string-1.3.0.jar"
BUNGEE="$ROOT/libraries/bungeecord-chat-1.20-R0.2.jar"
VERSION="3.3.25"
SRC="$HERE/src/main/java"
RES="$HERE/src/main/resources"
OUT="$HERE/build/classes"
JAR="$ROOT/plugins/dmz_adaptive_difficulty_gui-${VERSION}.jar"

for f in "$PAPER" "$PAPI" "$CMILIB" "$ADV_API" "$ADV_KEY" "$EXAM_API" "$EXAM_STR" "$BUNGEE"; do
  if [[ ! -f "$f" ]]; then
    echo "Missing $f" >&2
    exit 1
  fi
done

rm -f "$ROOT"/plugins/dmz_adaptive_difficulty_gui-*.jar
rm -rf "$OUT"
mkdir -p "$OUT"

mapfile -t SOURCES < <(find "$SRC" -name '*.java' | sort)
CP="$PAPER:$PAPI:$CMILIB:$ADV_API:$ADV_KEY:$EXAM_API:$EXAM_STR:$BUNGEE"
javac --release 17 -proc:none -cp "$CP" -d "$OUT" "${SOURCES[@]}"

(
  cd "$OUT"
  jar cf "$JAR" $(find com -name '*.class')
)
jar uf "$JAR" -C "$RES" plugin.yml
echo "Built $JAR"
jar tf "$JAR"
