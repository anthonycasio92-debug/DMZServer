#!/usr/bin/env bash
# Package the Noea planet-explosion coremod. No Java classes are compiled.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
SRC="$HERE/src"
OUT="$ROOT/mods/noea-explosion-no-tick.jar"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT

rm -f "$OUT"
mkdir -p "$ROOT/mods" "$STAGE/META-INF" "$STAGE/coremods"
cp "$SRC/META-INF/mods.toml" "$STAGE/META-INF/mods.toml"
cp "$SRC/META-INF/coremods.json" "$STAGE/META-INF/coremods.json"
cp "$SRC/coremods/noea_explosion_no_tick.js" "$STAGE/coremods/noea_explosion_no_tick.js"
(cd "$STAGE" && jar cf "$OUT" META-INF/mods.toml META-INF/coremods.json coremods/noea_explosion_no_tick.js)

python3 - "$OUT" "${NOEA_JAR:-}" << 'PY'
import sys, zipfile
from pathlib import Path

jar = Path(sys.argv[1])
noea = Path(sys.argv[2]) if len(sys.argv) > 2 and sys.argv[2] else None
need = {
    "META-INF/mods.toml",
    "META-INF/coremods.json",
    "coremods/noea_explosion_no_tick.js",
}
with zipfile.ZipFile(jar) as zf:
    names = set(zf.namelist())
    classes = [n for n in names if n.endswith(".class")]
    js = zf.read("coremods/noea_explosion_no_tick.js").decode("utf-8")
    toml = zf.read("META-INF/mods.toml").decode("utf-8")
    core = zf.read("META-INF/coremods.json").decode("utf-8")
missing = sorted(need - names)
if missing or classes:
    print("ERROR: coremod jar layout", file=sys.stderr)
    for name in missing:
        print("  missing " + name, file=sys.stderr)
    for name in classes:
        print("  unexpected class " + name, file=sys.stderr)
    sys.exit(1)
for token in (
    "m_8119_",
    "DeepSpaceExplosionEntity",
    "handlePlanetDetonationTick",
    "noea_explosion_no_tick.js",
    'modId="noeaexplosionnotick"',
    'version="1.0.3"',
    "insertBefore",
    "m_146870_",
    "DiscardExplosionOnSpawn",
    "<init>",
    "INVOKESPECIAL",
):
    blob = js + toml + core
    if token not in blob:
        print("ERROR: jar missing " + token, file=sys.stderr)
        sys.exit(1)
if "instructions.clear" in js or "tryCatchBlocks.clear" in js:
    print("ERROR: clearing a method corrupts its stack map and the server will not start", file=sys.stderr)
    sys.exit(1)
if "m_8097_" in js or "CelestialDestructionService" in js:
    print("ERROR: destruction start must keep running so the planet can disappear", file=sys.stderr)
    sys.exit(1)
if noea and noea.is_file():
    import subprocess
    targets = [
        ("com.butterjaffa.noeabosses.entity.DeepSpaceExplosionEntity", "m_8119_()"),
        ("com.butterjaffa.noeabosses.entity.DeepSpaceExplosionEntity", "DeepSpaceExplosionEntity(net.minecraft.world.entity.EntityType"),
        ("com.butterjaffa.noeabosses.DeepSpaceEvents", "handlePlanetDetonationTick("),
    ]
    seen = {}
    with zipfile.ZipFile(noea) as zf:
        for cls, sig in targets:
            path = cls.replace(".", "/") + ".class"
            if path not in zf.namelist():
                print("ERROR: Noea jar missing " + path, file=sys.stderr)
                sys.exit(1)
            if path not in seen:
                data = zf.read(path)
                tmp = Path("/tmp") / path.replace("/", "_")
                tmp.write_bytes(data)
                seen[path] = subprocess.check_output(["javap", "-p", str(tmp)], text=True)
            if sig not in seen[path]:
                print("ERROR: " + cls + " has no " + sig, file=sys.stderr)
                sys.exit(1)
    print("Noea method targets present")
print("Coremod jar ok:", jar)
PY
echo "Built $OUT"
