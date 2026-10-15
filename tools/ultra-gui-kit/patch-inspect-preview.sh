#!/usr/bin/env bash
# Build the bridge client that honors layoutJson.playerModel.subject.
# Stock renderPreview always draws Minecraft.player. This jar keeps that path for "local"
# and draws the named player (model, skin, synced parts) for any mod that sets a UUID.
#
# Usage: bash tools/ultra-gui-kit/patch-inspect-preview.sh
# Optional:
#   KAIPATH_BASE_JAR=mods/kaipath-1.3.19.jar
#   KAIPATH_SUBJECT_VERSION=1.3.20
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
BASE_JAR="${KAIPATH_BASE_JAR:-$ROOT/mods/kaipath-1.3.19.jar}"
GSON="$ROOT/libraries/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
ASM="$ROOT/libraries/org/ow2/asm/asm/9.8/asm-9.8.jar"
STUB_SRC="$HERE/stubs"
OUT="$HERE/build/inspect-preview"
STUBS="$OUT/stubs"
CLASSES="$OUT/classes"
PATCH_CLASSES="$OUT/patch"
TEST_CLASSES="$OUT/test"
VERSION="${KAIPATH_SUBJECT_VERSION:-1.3.20}"
OUT_JAR="$ROOT/mods/kaipath-${VERSION}.jar"

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
python3 - "$OUT_JAR" "$VERSION" <<'PY'
import pathlib, sys, zipfile, io
jar_path, version = sys.argv[1], sys.argv[2]
buf = io.BytesIO(pathlib.Path(jar_path).read_bytes())
src = zipfile.ZipFile(buf)
out = io.BytesIO()
with zipfile.ZipFile(out, "w") as dest:
    for info in src.infolist():
        data = src.read(info.filename)
        if info.filename == "META-INF/MANIFEST.MF":
            text = data.decode("utf-8", "replace")
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
            data = "\r\n".join(lines).encode("utf-8")
        dest.writestr(info, data)
pathlib.Path(jar_path).write_bytes(out.getvalue())
PY
echo "Built $(basename "$OUT_JAR") — playerModel.subject draws that player; local stays the stock viewer."
