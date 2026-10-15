#!/usr/bin/env bash
# Patch stock KaiPath so Character Services draws an inspect subject.
# Stock 1.3.17 renderPreview always passes Minecraft.player to InventoryScreen.
# This leaves that path in place and returns early when playerModel.subject is another uuid.
#
# Usage: bash tools/ultra-gui-kit/patch-inspect-preview.sh
# Optional: KAIPATH_BASE_JAR=mods/kaipath-1.3.17.jar
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
BASE_JAR="${KAIPATH_BASE_JAR:-$ROOT/mods/kaipath-1.3.17.jar}"
GSON="$ROOT/libraries/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
ASM="$ROOT/libraries/org/ow2/asm/asm/9.8/asm-9.8.jar"
STUB_SRC="$HERE/stubs"
OUT="$HERE/build/inspect-preview"
STUBS="$OUT/stubs"
CLASSES="$OUT/classes"
PATCH_CLASSES="$OUT/patch"
TEST_CLASSES="$OUT/test"
OUT_JAR="$ROOT/mods/kaipath-1.3.17-inspect-subject.jar"

if [[ ! -f "$BASE_JAR" ]]; then
  echo "Missing base KaiPath jar: $BASE_JAR" >&2
  exit 1
fi

rm -rf "$OUT"
mkdir -p "$STUBS" "$CLASSES" "$PATCH_CLASSES" "$TEST_CLASSES"
python3 "$STUB_SRC/gen_stubs.py"
find "$STUB_SRC" -name '*.java' > "$OUT/stubs.list"
javac --release 17 -d "$STUBS" @"$OUT/stubs.list"

javac --release 17 -cp "$STUBS:$BASE_JAR:$GSON" -d "$CLASSES" \
  "$HERE/kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/InspectPreviewSubjects.java" \
  "$HERE/kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/InspectPreview.java"

javac --release 17 -cp "$CLASSES:$GSON" -d "$TEST_CLASSES" \
  "$HERE/test/com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/InspectPreviewSubjectTest.java"
java -cp "$TEST_CLASSES:$CLASSES:$GSON" \
  com.dmzlegacy.kaipath.ultra.consumers.legacymechanics.InspectPreviewSubjectTest

javac --release 17 -cp "$ASM" -d "$PATCH_CLASSES" \
  "$HERE/patch/PatchInspectPreview.java"
java -cp "$PATCH_CLASSES:$ASM" com.dbzlegacy.kaipath.patch.PatchInspectPreview \
  "$BASE_JAR" \
  "$OUT_JAR" \
  "$CLASSES/com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/InspectPreview.class" \
  "$CLASSES/com/dmzlegacy/kaipath/ultra/consumers/legacymechanics/InspectPreviewSubjects.class"

javap -c -p -classpath "$OUT_JAR" \
  com.dmzlegacy.kaipath.ultra.consumers.legacymechanics.LmDynamicScreen \
  | awk '
      /void renderPreview/ { show=1 }
      show { print }
      show && /^  [a-zA-Z].*\(/ && !/void renderPreview/ { exit }
    ' > "$OUT/renderPreview.javap"

if ! grep -q 'InspectPreview.tryRender' "$OUT/renderPreview.javap"; then
  echo "Patched renderPreview does not call InspectPreview.tryRender" >&2
  cat "$OUT/renderPreview.javap" >&2
  exit 1
fi
if ! grep -q 'Minecraft.f_91074_' "$OUT/renderPreview.javap"; then
  echo "Patched renderPreview dropped the stock local-player path" >&2
  exit 1
fi
# Other methods stay stock: preview() still builds the same Preview record.
if ! javap -p -classpath "$OUT_JAR" \
  com.dmzlegacy.kaipath.ultra.consumers.legacymechanics.LmDynamicScreen \
  | grep -q 'Preview preview(com.google.gson.JsonObject)'; then
  echo "preview() missing after patch" >&2
  exit 1
fi
echo "Patched $(basename "$OUT_JAR") — renderPreview calls InspectPreview.tryRender, then the stock local player."
