#!/usr/bin/env bash
# Build a KaiPath jar with LM Ultra dynamic GUI client (10-field GuiOpenPacket).
# Overlay replaces com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/* on kaipath-1.3.0.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
BASE_JAR="${KAIPATH_BASE_JAR:-$HERE/base/kaipath-1.3.0.jar}"
GSON="$ROOT/libraries/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
LOG4J="$ROOT/libraries/org/apache/logging/log4j/log4j-api/2.19.0/log4j-api-2.19.0.jar"
OUT_DIR="$HERE/build"
CLASSES="$OUT_DIR/classes"
STUB_CLASSES="$OUT_DIR/stub-classes"
SRC="$HERE/src/main/java"
VERSION="${KAIPATH_PATCH_VERSION:-1.3.1}"
OUT_JAR="$ROOT/mods/kaipath-${VERSION}-lm-ultra.jar"

if [[ ! -f "$BASE_JAR" ]]; then
  echo "Missing base KaiPath jar: $BASE_JAR" >&2
  exit 1
fi
if [[ ! -f "$GSON" ]]; then
  echo "Missing gson: $GSON" >&2
  exit 1
fi
if [[ ! -f "$LOG4J" ]]; then
  echo "Missing log4j-api: $LOG4J" >&2
  exit 1
fi

rm -rf "$OUT_DIR"
mkdir -p "$CLASSES" "$STUB_CLASSES"

python3 "$HERE/stubs/gen_stubs.py"
# Prefer real log4j; drop stub log4j sources if present
find "$HERE/stubs" -path '*/org/apache/logging/*' -delete 2>/dev/null || true
find "$HERE/stubs" -name '*.java' > "$OUT_DIR/stubs.list"
javac --release 17 -d "$STUB_CLASSES" @"$OUT_DIR/stubs.list"

find "$SRC" -name '*.java' > "$OUT_DIR/sources.list"
CP="$STUB_CLASSES:$BASE_JAR:$GSON:$LOG4J"
javac --release 17 -cp "$CP" -d "$CLASSES" @"$OUT_DIR/sources.list"

# Offline round-trip self-test (uses in-memory FriendlyByteBuf stub)
TEST_SRC="$HERE/src/test/java"
if [[ -d "$TEST_SRC" ]]; then
  find "$TEST_SRC" -name '*.java' > "$OUT_DIR/tests.list"
  TEST_CLASSES="$OUT_DIR/test-classes"
  mkdir -p "$TEST_CLASSES"
  javac --release 17 -cp "$CLASSES:$CP" -d "$TEST_CLASSES" @"$OUT_DIR/tests.list"
  java -cp "$TEST_CLASSES:$CLASSES:$CP" \
    com.dmzlegacy.kaipath.ultra.consumers.legacymechanics.GuiOpenPacketRoundTripTest
fi

# Overlay onto a copy of the base jar
cp -f "$BASE_JAR" "$OUT_JAR"
# Remove old consumer classes then add new ones (incl. LmDynamicScreen)
jar uf "$OUT_JAR" -C "$CLASSES" com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/

# Verify critical classes
for c in GuiOpenPacket GuiActionPacket LmGuiNetwork LmScreens LmDynamicScreen LmHubScreen LmUltraConsumer; do
  if ! jar tf "$OUT_JAR" | grep -q "consumers/legacymechanics/${c}.class"; then
    echo "ERROR: missing $c in $OUT_JAR" >&2
    exit 1
  fi
done

javap -classpath "$OUT_JAR" -public com.dmzlegacy.kaipath.ultra.consumers.legacymechanics.GuiOpenPacket \
  | tee "$OUT_DIR/GuiOpenPacket.javap.txt"
if ! grep -q 'panelWidth' "$OUT_DIR/GuiOpenPacket.javap.txt"; then
  echo "ERROR: GuiOpenPacket missing panelWidth accessor" >&2
  exit 1
fi

echo "Built $OUT_JAR"
jar tf "$OUT_JAR" | grep 'consumers/legacymechanics/' | sort
