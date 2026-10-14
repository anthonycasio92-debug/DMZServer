#!/usr/bin/env bash
# Dev-only smoke: overlay tools/ultra-gui-kit/kaipath-client onto a released KaiPath jar.
# Adds headerBottom/accent/palette/sound/buttonSkin/frame/pagedBook honor+advertise
# (and field 11 / textInput if base is older).
# Official KaiPath releases are owned by the KaiPath mod developer — do NOT deploy this
# overlay to live; merge kaipath-client/ into the next official jar instead.
#
# Usage:
#   bash tools/ultra-gui-kit/build-kaipath-overlay.sh
# Optional:
#   KAIPATH_BASE_JAR=mods/kaipath-1.3.9.jar
#   KAIPATH_OVERLAY_VERSION=1.3.9-lm-ultra
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
BASE_JAR="${KAIPATH_BASE_JAR:-$ROOT/mods/kaipath-1.3.9.jar}"
GSON="$ROOT/libraries/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
LOG4J="$ROOT/libraries/org/apache/logging/log4j/log4j-api/2.19.0/log4j-api-2.19.0.jar"
OUT_DIR="$HERE/build"
CLASSES="$OUT_DIR/classes"
STUB_CLASSES="$OUT_DIR/stub-classes"
SRC="$HERE/kaipath-client"
TEST_SRC="$HERE/test"
VERSION="${KAIPATH_OVERLAY_VERSION:-1.3.9-lm-ultra}"
OUT_JAR="$ROOT/mods/kaipath-${VERSION}.jar"

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

if [[ -d "$TEST_SRC" ]]; then
  find "$TEST_SRC" -name '*.java' > "$OUT_DIR/tests.list"
  TEST_CLASSES="$OUT_DIR/test-classes"
  mkdir -p "$TEST_CLASSES"
  javac --release 17 -cp "$CLASSES:$CP" -d "$TEST_CLASSES" @"$OUT_DIR/tests.list"
  java -cp "$TEST_CLASSES:$CLASSES:$CP" \
    com.dmzlegacy.kaipath.ultra.consumers.legacymechanics.GuiOpenPacketRoundTripTest
fi

cp -f "$BASE_JAR" "$OUT_JAR"

# Preserve + stamp MANIFEST before jar/zip rewrites (jar uf can drop it).
MANIFEST_DIR="$OUT_DIR/manifest"
mkdir -p "$MANIFEST_DIR/META-INF"
unzip -p "$BASE_JAR" META-INF/MANIFEST.MF > "$MANIFEST_DIR/META-INF/MANIFEST.MF"
python3 - "$MANIFEST_DIR/META-INF/MANIFEST.MF" "$VERSION" <<'PY'
import pathlib, sys
path = pathlib.Path(sys.argv[1])
version = sys.argv[2]
text = path.read_text(encoding="utf-8", errors="replace")
lines = []
seen = False
for line in text.splitlines():
    if line.startswith("Implementation-Version:"):
        lines.append(f"Implementation-Version: {version}")
        seen = True
    else:
        lines.append(line)
if not seen:
    lines.append(f"Implementation-Version: {version}")
if not lines or lines[-1] != "":
    lines.append("")
path.write_text("\r\n".join(lines), encoding="utf-8")
PY

# Drop prior consumer classes (incl. obsolete inner classes) then add kit overlay.
if command -v zip >/dev/null 2>&1; then
  zip -d "$OUT_JAR" \
    'com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/*' \
    'com/dmzlegacy/kaipath/ultra/consumers/UltraGuiConsumerRegistry.class' \
    'com/dmzlegacy/kaipath/ultra/consumers/UltraGuiConsumerRegistry$*.class' \
    'com/dmzlegacy/kaipath/ultra/consumers/UltraGuiPartners.class' \
    2>/dev/null || true
fi

jar uf "$OUT_JAR" -C "$CLASSES" com/dmzlegacy/kaipath/ultra/consumers/

# Fix stock fitLabel mojibake ellipsis (â€¦ → ...) that shows as weird A/E glyphs.
FIT_CLASS_DIR="$OUT_DIR/fitlabel-patch"
mkdir -p "$FIT_CLASS_DIR/com/dmzlegacy/kaipath/ultra/internal"
unzip -p "$OUT_JAR" com/dmzlegacy/kaipath/ultra/internal/UltraWidgetReflect.class \
  > "$FIT_CLASS_DIR/com/dmzlegacy/kaipath/ultra/internal/UltraWidgetReflect.class"
python3 "$HERE/patch_fitlabel_ascii.py" \
  "$FIT_CLASS_DIR/com/dmzlegacy/kaipath/ultra/internal/UltraWidgetReflect.class"
jar uf "$OUT_JAR" -C "$FIT_CLASS_DIR" com/dmzlegacy/kaipath/ultra/internal/UltraWidgetReflect.class
# Verify mojibake gone and ASCII ellipsis present
if unzip -p "$OUT_JAR" com/dmzlegacy/kaipath/ultra/internal/UltraWidgetReflect.class \
  | python3 -c "import sys; d=sys.stdin.buffer.read(); moj=bytes([0xC3,0xA2,0xE2,0x82,0xAC,0xC2,0xA6]);
sys.exit(1 if moj in d or b'...' not in d else 0)"; then
  echo "fitLabel ASCII ellipsis OK"
else
  echo "ERROR: UltraWidgetReflect still has mojibake ellipsis or missing ASCII ..." >&2
  exit 1
fi

# Restore stamped MANIFEST via zip (jar uf often drops META-INF/MANIFEST.MF).
(cd "$MANIFEST_DIR" && zip -q "$OUT_JAR" META-INF/MANIFEST.MF)
if ! jar tf "$OUT_JAR" | grep -q '^META-INF/MANIFEST.MF$'; then
  echo "ERROR: META-INF/MANIFEST.MF missing from $OUT_JAR" >&2
  jar tf "$OUT_JAR" | grep META-INF || true
  exit 1
fi
if ! unzip -p "$OUT_JAR" META-INF/MANIFEST.MF | grep -q "Implementation-Version: ${VERSION}"; then
  echo "ERROR: Implementation-Version not stamped to $VERSION" >&2
  unzip -p "$OUT_JAR" META-INF/MANIFEST.MF >&2 || true
  exit 1
fi
if ! jar tf "$OUT_JAR" | grep -q '^META-INF/mods.toml$'; then
  echo "ERROR: META-INF/mods.toml missing from $OUT_JAR" >&2
  exit 1
fi

for c in \
  GuiOpenPacket GuiActionPacket GuiFeaturesPacket \
  LmGuiNetwork LmScreens LmDynamicScreen LmHubScreen LmUltraConsumer; do
  if ! jar tf "$OUT_JAR" | grep -q "consumers/legacymechanics/${c}.class"; then
    echo "ERROR: missing $c in $OUT_JAR" >&2
    exit 1
  fi
done
if ! jar tf "$OUT_JAR" | grep -q 'consumers/UltraGuiConsumerRegistry.class'; then
  echo "ERROR: missing UltraGuiConsumerRegistry in $OUT_JAR" >&2
  exit 1
fi
if ! jar tf "$OUT_JAR" | grep -q 'consumers/UltraGuiPartners.class'; then
  echo "ERROR: missing UltraGuiPartners in $OUT_JAR" >&2
  exit 1
fi

javap -classpath "$OUT_JAR" -public \
  com.dmzlegacy.kaipath.ultra.consumers.legacymechanics.GuiOpenPacket \
  | tee "$OUT_DIR/GuiOpenPacket.javap.txt"
if ! grep -q 'inputsJson' "$OUT_DIR/GuiOpenPacket.javap.txt"; then
  echo "ERROR: GuiOpenPacket missing inputsJson accessor" >&2
  exit 1
fi

# Confirm textInput advertise string is in the registry class bytes
if ! strings "$OUT_JAR" | grep -q 'textInput'; then
  # fallback: javap / strings on extracted class
  if ! jar xf "$OUT_JAR" com/dmzlegacy/kaipath/ultra/consumers/UltraGuiConsumerRegistry.class \
    && strings com/dmzlegacy/kaipath/ultra/consumers/UltraGuiConsumerRegistry.class | grep -q textInput; then
    echo "ERROR: UltraGuiConsumerRegistry does not embed textInput advertise" >&2
    exit 1
  fi
  rm -f com/dmzlegacy/kaipath/ultra/consumers/UltraGuiConsumerRegistry.class
  rmdir -p com/dmzlegacy/kaipath/ultra/consumers 2>/dev/null || true
fi

echo "Built $OUT_JAR"
jar tf "$OUT_JAR" | grep -E 'consumers/(UltraGuiConsumerRegistry|UltraGuiPartners|legacymechanics/)' | sort
echo "Advertise: $(python3 - <<PY
import zipfile
z=zipfile.ZipFile("$OUT_JAR")
data=z.read("com/dmzlegacy/kaipath/ultra/consumers/UltraGuiConsumerRegistry.class")
# rough extract of UTF-8 string constants containing textInput
import re
for m in re.findall(rb'\[[^\]]{0,200}textInput[^\]]{0,80}\]', data):
    print(m.decode("utf-8", "replace"))
    break
else:
    print("textInput present in class bytes")
PY
)"
