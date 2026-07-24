#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRG="$ROOT/libraries/net/minecraft/server/1.20.1-20230612.114412/server-1.20.1-20230612.114412-srg.jar"
FORGE_U="$ROOT/libraries/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-universal.jar"
FORGE_S="$ROOT/libraries/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-server.jar"
DMZ="$ROOT/mods/dragonminez-2.1.3.jar"
SHURUI="${SHURUI_JAR:-}"
if [[ -z "$SHURUI" ]]; then
  for candidate in \
    "$ROOT/mods"/shuruis_raid_bosses-*.jar \
    /tmp/mohist_test/mods/shuruis_raid_bosses-*.jar \
    /tmp/shurui_raid/shuruis_raid_bosses-*.jar
  do
    if [[ -f "$candidate" ]]; then
      SHURUI="$candidate"
      break
    fi
  done
fi
SRC="$(cd "$(dirname "$0")" && pwd)/src/main/java"
RES="$(cd "$(dirname "$0")" && pwd)/src/main/resources"
OUT="$(cd "$(dirname "$0")" && pwd)/build/classes"
JAR="$ROOT/mods/dmz_mohist_melee_fix-2.9.0.jar"
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-1.*.jar
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-2.0.*.jar
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-2.1.*.jar
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-2.2.*.jar
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-2.3.*.jar
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-2.4.*.jar
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-2.5.*.jar
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-2.6.*.jar
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-2.7.*.jar
rm -f "$ROOT"/mods/dmz_mohist_melee_fix-2.8.*.jar

CP="$SRG:$FORGE_S:$FORGE_U:$ROOT/libraries/net/minecraftforge/fmlcore/1.20.1-47.4.10/fmlcore-1.20.1-47.4.10.jar:$ROOT/libraries/net/minecraftforge/fmlloader/1.20.1-47.4.10/fmlloader-1.20.1-47.4.10.jar:$ROOT/libraries/net/minecraftforge/forgespi/7.0.1/forgespi-7.0.1.jar:$ROOT/libraries/net/minecraftforge/javafmllanguage/1.20.1-47.4.10/javafmllanguage-1.20.1-47.4.10.jar:$ROOT/libraries/net/minecraftforge/eventbus/6.0.5/eventbus-6.0.5.jar:$ROOT/libraries/org/spongepowered/mixin/0.8.5/mixin-0.8.5.jar:$ROOT/libraries/org/ow2/asm/asm-tree/9.6/asm-tree-9.6.jar:$ROOT/libraries/org/ow2/asm/asm/9.6/asm-9.6.jar:$ROOT/libraries/org/apache/logging/log4j/log4j-api/2.19.0/log4j-api-2.19.0.jar:$ROOT/libraries/com/mojang/authlib/4.0.43/authlib-4.0.43.jar:$ROOT/libraries/com/google/guava/guava/31.1-jre/guava-31.1-jre.jar:$DMZ"
if [[ -n "$SHURUI" ]]; then
  CP="$CP:$SHURUI"
  echo "Using Shurui jar for compile: $SHURUI"
else
  echo "WARN: shuruis_raid_bosses jar not found; raid mixin may fail to compile" >&2
fi

rm -rf "$OUT"
mkdir -p "$OUT"
javac --release 17 -proc:none -cp "$CP" -d "$OUT" \
  "$SRC"/com/dbzlegacy/mohistmelee/*.java \
  "$SRC"/com/dbzlegacy/mohistmelee/mixin/*.java

echo '{}' > /tmp/dmz_mohist_melee_fix.refmap.json
(
  cd "$OUT"
  jar cvmf "$RES/META-INF/MANIFEST.MF" "$JAR" $(find com -name '*.class') \
    -C "$RES" META-INF/mods.toml \
    -C "$RES" dmz_mohist_melee_fix.mixins.json \
    -C "$RES" dmz_mohist_melee_fix.raid.mixins.json \
    -C "$RES" pack.mcmeta
)
jar uf "$JAR" -C /tmp dmz_mohist_melee_fix.refmap.json
echo "Built $JAR"
jar tf "$JAR"
