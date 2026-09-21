#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRG="$ROOT/libraries/net/minecraft/server/1.20.1-20230612.114412/server-1.20.1-20230612.114412-srg.jar"
FORGE_U="$ROOT/libraries/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-universal.jar"
FORGE_S="$ROOT/libraries/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-server.jar"
DMZ="$ROOT/mods/dragonminez-2.1.3.jar"
GSON="$ROOT/libraries/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
LIGHTMANS="$ROOT/libraries/lightmanscurrency-1.20.1-2.3.0.5.jar"
if [[ ! -f "$LIGHTMANS" ]]; then
  LIGHTMANS="$ROOT/mods/lightmanscurrency-1.20.1-2.3.0.5.jar"
fi
FTB="$ROOT/libraries/ftb-teams-forge-2001.3.1.jar"
FTB_CHUNKS="$ROOT/libraries/ftb-chunks-forge-2001.3.8.jar"
FTB_LIBRARY="$ROOT/libraries/ftb-library-forge-2001.2.13.jar"
ARCHITECTURY="$ROOT/libraries/architectury-9.2.14-forge.jar"
REVAMP="$ROOT/libraries/dmzrevamp-2.0.9.jar"
if [[ ! -f "$FTB" ]]; then
  echo "Downloading FTB Teams for compile..."
  curl -fsSL -o "$FTB" \
    "https://maven.ftb.dev/releases/dev/ftb/mods/ftb-teams-forge/2001.3.1/ftb-teams-forge-2001.3.1.jar"
fi
for dep in "$FTB_CHUNKS" "$FTB_LIBRARY" "$ARCHITECTURY" "$REVAMP"; do
  if [[ ! -f "$dep" ]]; then
    echo "Missing $dep — copy from live mods/ or libraries/" >&2
    exit 1
  fi
done
NAME="LegacyMechanics"
SRC="$(cd "$(dirname "$0")" && pwd)/src/main/java"
MOD_JAVA="$SRC/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java"
VERSION="$(grep -oP 'public static final String VERSION = "\K[0-9.]+(?=")' "$MOD_JAVA")"
if [[ -z "$VERSION" ]]; then
  echo "Could not read VERSION from $MOD_JAVA" >&2
  exit 1
fi
RES="$(cd "$(dirname "$0")" && pwd)/src/main/resources"
OUT="$(cd "$(dirname "$0")" && pwd)/build/classes"
JAR="$ROOT/mods/${NAME}-${VERSION}.jar"

# Replace only this version's jar (keep older LegacyMechanics-x.y.z in mods/ for history).
rm -f "$JAR" \
      "$ROOT"/mods/AdaptiveDifficulty-"${VERSION}".jar \
      "$ROOT"/mods/dmz_adaptive_difficulty-"${VERSION}".jar

CP="$SRG:$FORGE_S:$FORGE_U:$GSON:$LIGHTMANS:$FTB:$FTB_CHUNKS:$FTB_LIBRARY:$ARCHITECTURY:$REVAMP:\
$ROOT/libraries/net/minecraftforge/fmlcore/1.20.1-47.4.10/fmlcore-1.20.1-47.4.10.jar:\
$ROOT/libraries/net/minecraftforge/fmlloader/1.20.1-47.4.10/fmlloader-1.20.1-47.4.10.jar:\
$ROOT/libraries/net/minecraftforge/forgespi/7.0.1/forgespi-7.0.1.jar:\
$ROOT/libraries/net/minecraftforge/javafmllanguage/1.20.1-47.4.10/javafmllanguage-1.20.1-47.4.10.jar:\
$ROOT/libraries/net/minecraftforge/eventbus/6.0.5/eventbus-6.0.5.jar:\
$ROOT/libraries/org/ow2/asm/asm-tree/9.6/asm-tree-9.6.jar:\
$ROOT/libraries/org/ow2/asm/asm/9.6/asm-9.6.jar:\
$ROOT/libraries/org/spongepowered/mixin/0.8.5/mixin-0.8.5.jar:\
$ROOT/libraries/org/apache/logging/log4j/log4j-api/2.19.0/log4j-api-2.19.0.jar:\
$ROOT/libraries/com/mojang/authlib/4.0.43/authlib-4.0.43.jar:\
$ROOT/libraries/com/mojang/brigadier/1.1.8/brigadier-1.1.8.jar:\
$ROOT/libraries/com/google/guava/guava/31.1-jre/guava-31.1-jre.jar:\
$ROOT/libraries/org/slf4j/slf4j-api/2.0.1/slf4j-api-2.0.1.jar:\
$DMZ"

rm -rf "$OUT"
mkdir -p "$OUT"

mapfile -t SOURCES < <(find "$SRC" -name '*.java' | sort)
javac --release 17 -proc:none -cp "$CP" -d "$OUT" "${SOURCES[@]}"

(
  cd "$OUT"
  jar cvmf "$RES/META-INF/MANIFEST.MF" "$JAR" $(find com -name '*.class') \
    -C "$RES" META-INF/mods.toml \
    -C "$RES" pack.mcmeta \
    -C "$RES" legacymechanics.mixins.json \
    -C "$RES" legacymechanics.refmap.json
)

# Do NOT overlay DmzResourcePoolClamp.class from an older jar — mixed bytecode breaks
# Java 17 verification when mixins call actualMaxEnergy from woven StatsData (VerifyError → invalid playerdata).

echo "Built $JAR"
jar tf "$JAR"

# Fail-closed audits: product features, combat scaling sim, GUI ABI.
HERE_SIM="$(cd "$(dirname "$0")" && pwd)/sim"
python3 "$HERE_SIM/audit_scaling_sync.py"
python3 "$HERE_SIM/audit_features.py"
python3 "$HERE_SIM/audit_gui_tooltips.py"
python3 "$HERE_SIM/audit_form_bands.py"
python3 "$HERE_SIM/validate_tier_costs.py"
python3 "$HERE_SIM/audit_tier_level_matrix.py"
python3 "$HERE_SIM/validate_scaling.py"
python3 "$HERE_SIM/simulate_build_matrix.py" --check
python3 "$HERE_SIM/audit_concept.py"
if [[ -f "$ROOT/plugins/LegacyMechanicsGUI-${VERSION}.jar" ]] || [[ -f "$ROOT/mods/LegacyMechanics-${VERSION}.jar" ]]; then
  python3 "$HERE_SIM/audit_gui_abi.py"
else
  echo "WARN: LegacyMechanicsGUI-${VERSION}.jar missing — skip GUI ABI audit" >&2
fi
