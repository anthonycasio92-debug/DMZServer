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
GSON="$ROOT/libraries/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
GP="$ROOT/libraries/GriefPrevention.jar"
VERSION="2.4.55"
NAME="LegacyMechanicsGUI"
SRC="$HERE/src/main/java"
RES="$HERE/src/main/resources"
OUT="$HERE/build/classes"
JAR="$ROOT/plugins/${NAME}-${VERSION}.jar"

for f in "$PAPER" "$PAPI" "$CMILIB" "$ADV_API" "$ADV_KEY" "$EXAM_API" "$EXAM_STR" "$BUNGEE" "$GSON" "$GP"; do
  if [[ ! -f "$f" ]]; then
    echo "Missing $f" >&2
    exit 1
  fi
done

# Only one GUI companion jar — drop old AdaptiveDifficultyGUI / LegacyMechanicsGUI builds.
rm -f "$ROOT"/plugins/LegacyMechanicsGUI-*.jar \
      "$ROOT"/plugins/AdaptiveDifficultyGUI-*.jar \
      "$ROOT"/plugins/dmz_adaptive_difficulty_gui-*.jar \
      "$ROOT"/plugins/DMZAdaptiveDifficultyGUI-*.jar
rm -rf "$OUT"
mkdir -p "$OUT"

mapfile -t SOURCES < <(find "$SRC" -name '*.java' | sort)
CP="$PAPER:$PAPI:$CMILIB:$ADV_API:$ADV_KEY:$EXAM_API:$EXAM_STR:$BUNGEE:$GSON:$GP"
javac --release 17 -proc:none -cp "$CP" -d "$OUT" "${SOURCES[@]}"

# Shade Gson into the plugin jar (Paper may not expose it to plugins).
(
  cd "$OUT"
  jar xf "$GSON"
  rm -rf META-INF/maven META-INF/MANIFEST.MF 2>/dev/null || true
  jar cf "$JAR" $(find com -type f | sort)
)
jar uf "$JAR" -C "$RES" plugin.yml
jar uf "$JAR" -C "$RES" gui-tooltips.json
echo "Built $JAR"
jar tf "$JAR" | head -40

# Cross-check Forge reflection surface after GUI rebuild.
if [[ -f "$ROOT/mods/LegacyMechanics-${VERSION}.jar" ]]; then
  python3 "$ROOT/tools/dmz-adaptive-difficulty/sim/audit_gui_abi.py"
else
  echo "WARN: LegacyMechanics-${VERSION}.jar missing — skip GUI ABI audit" >&2
fi