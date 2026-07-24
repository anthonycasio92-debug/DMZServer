#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRG="$ROOT/libraries/net/minecraft/server/1.20.1-20230612.114412/server-1.20.1-20230612.114412-srg.jar"
FORGE_U="$ROOT/libraries/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-universal.jar"
DMZ="$ROOT/mods/dragonminez-2.1.3.jar"
SRC="$(cd "$(dirname "$0")" && pwd)/src/main/java"
RES="$(cd "$(dirname "$0")" && pwd)/src/main/resources"
OUT="$(cd "$(dirname "$0")" && pwd)/build/classes"
JAR="$ROOT/mods/dmz_mohist_melee_fix-1.0.6.jar"
rm -f "$ROOT/mods"/dmz_mohist_melee_fix-1.0.[0-5].jar

CP="$SRG:$FORGE_U:\
$ROOT/libraries/net/minecraftforge/fmlcore/1.20.1-47.4.10/fmlcore-1.20.1-47.4.10.jar:\
$ROOT/libraries/net/minecraftforge/fmlloader/1.20.1-47.4.10/fmlloader-1.20.1-47.4.10.jar:\
$ROOT/libraries/net/minecraftforge/forgespi/7.0.1/forgespi-7.0.1.jar:\
$ROOT/libraries/net/minecraftforge/javafmllanguage/1.20.1-47.4.10/javafmllanguage-1.20.1-47.4.10.jar:\
$ROOT/libraries/net/minecraftforge/eventbus/6.0.5/eventbus-6.0.5.jar:\
$ROOT/libraries/org/spongepowered/mixin/0.8.5/mixin-0.8.5.jar:\
$ROOT/libraries/org/apache/logging/log4j/log4j-api/2.19.0/log4j-api-2.19.0.jar:\
$ROOT/libraries/com/mojang/authlib/4.0.43/authlib-4.0.43.jar:\
$ROOT/libraries/com/google/guava/guava/31.1-jre/guava-31.1-jre.jar:\
$DMZ"

rm -rf "$OUT"
mkdir -p "$OUT"
javac --release 17 -proc:none -cp "$CP" -d "$OUT" \
  "$SRC"/com/dbzlegacy/mohistmelee/*.java \
  "$SRC"/com/dbzlegacy/mohistmelee/mixin/*.java

echo '{}' > /tmp/dmz_mohist_melee_fix.refmap.json
cd "$OUT"
CLASSES=$(find com -name '*.class' | tr '\n' ' ')
jar cvmf "$RES/META-INF/MANIFEST.MF" "$JAR" $CLASSES \
  -C "$RES" META-INF/mods.toml \
  -C "$RES" dmz_mohist_melee_fix.mixins.json \
  -C "$RES" pack.mcmeta
cd /tmp && jar uf "$JAR" dmz_mohist_melee_fix.refmap.json
echo "Built $JAR"
jar tf "$JAR"
