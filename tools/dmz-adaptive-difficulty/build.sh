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
CNPC="$ROOT/mods/CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260227.jar"
if [[ ! -f "$FTB" ]]; then
  echo "Downloading FTB Teams for compile..."
  curl -fsSL -o "$FTB" \
    "https://maven.ftb.dev/releases/dev/ftb/mods/ftb-teams-forge/2001.3.1/ftb-teams-forge-2001.3.1.jar"
fi
for dep in "$FTB_CHUNKS" "$FTB_LIBRARY" "$ARCHITECTURY" "$REVAMP" "$CNPC"; do
  if [[ ! -f "$dep" ]]; then
    echo "Missing $dep — copy from live mods/ or libraries/" >&2
    exit 1
  fi
done
NAME="LegacyMechanics"
HERE="$(cd "$(dirname "$0")" && pwd)"
SRC="$HERE/src/main/java"
MOD_JAVA="$SRC/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java"
VERSION="$(grep -oP 'public static final String VERSION = "\K[0-9.]+(?=")' "$MOD_JAVA")"
if [[ -z "$VERSION" ]]; then
  echo "Could not read VERSION from $MOD_JAVA" >&2
  exit 1
fi
RES="$HERE/src/main/resources"
OUT="$HERE/build/classes"
CONSOLIDATED_BASE="$HERE/base/LegacyMechanics-4.5.49-consolidated.jar"
LEGACY_BASE="$HERE/base/LegacyMechanics-4.5.23-direct-dmz-resource-max.jar"
BASE_JAR="${LM_BASE_JAR:-$CONSOLIDATED_BASE}"
if [[ ! -f "$BASE_JAR" && -f "$LEGACY_BASE" ]]; then
  BASE_JAR="$LEGACY_BASE"
fi
# Overlay CNPC/GUI patches onto consolidated base; ship as LegacyMechanics-${VERSION}.jar
if [[ -f "$BASE_JAR" ]]; then
  JAR="$ROOT/mods/${NAME}-${VERSION}.jar"
else
  JAR="$ROOT/mods/${NAME}-${VERSION}.jar"
fi

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
$CNPC:\
$DMZ"

rm -rf "$OUT"
mkdir -p "$OUT"

mapfile -t SOURCES < <(find "$SRC" -name '*.java' | sort)
javac --release 17 -proc:none -cp "$CP" -d "$OUT" "${SOURCES[@]}"

merge_onto_base_jar() {
  local base="$1" dest="$2"
  local tmp merge
  tmp="$(mktemp -d)"
  merge="$(mktemp -d)"
  unzip -q "$base" -d "$merge"
  # Overlay selected compiled packages (CNPC GUI work) — everything else stays from live base jar.
  local rel
  if [[ -d "$OUT/com/dbzlegacy/adaptivedifficulty/gui/cnpc" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/gui/cnpc"
    cp -a "$OUT/com/dbzlegacy/adaptivedifficulty/gui/cnpc/." "$merge/com/dbzlegacy/adaptivedifficulty/gui/cnpc/"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/gui/RivalGuiApi.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/gui/RivalGuiApi.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/gui/RivalGuiApi.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/rival/RivalSystem.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/rival/RivalSystem.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/rival/RivalSystem.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/gui/RivalGuiApi.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/gui/RivalGuiApi.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/gui/RivalGuiApi.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/rival/RivalChallengeManager.class" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/rival"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/rival/RivalChallengeManager.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/rival/RivalChallengeManager.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/rival/RivalStore.class" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/rival"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/rival/RivalStore.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/rival/RivalStore.class"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/rival/RivalStore\$"*.class \
      "$merge/com/dbzlegacy/adaptivedifficulty/rival/" 2>/dev/null || true
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/gui/SparGuiApi.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/gui/SparGuiApi.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/gui/SparGuiApi.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/sparring/DojoRankings.class" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/sparring"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/sparring/DojoRankings.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/sparring/DojoRankings.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/gui/ProgressionGuiApi.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/gui/ProgressionGuiApi.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/gui/ProgressionGuiApi.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.class" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/progression/shop"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.class"
  fi
  # Hub/chat fallback + /lm open|page — must ship from src (base jar may still reference /lmdo).
  for class in MechanicsChatMenu DifficultyChatMenu RivalChatMenu SparChatMenu ProgressionChatMenu; do
    if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/gui/${class}.class" ]]; then
      cp "$OUT/com/dbzlegacy/adaptivedifficulty/gui/${class}.class" \
        "$merge/com/dbzlegacy/adaptivedifficulty/gui/${class}.class"
    fi
  done
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.class" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/command"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.class"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands\$"*.class \
      "$merge/com/dbzlegacy/adaptivedifficulty/command/" 2>/dev/null || true
  fi
  for class in LmCommandSuggestions LmCommandFeedback LmStaffHelp LmCommandHelp LmCommandMessages CommandAccess LmAdminArgCoalesce MohistCommandBridge PrestigeAdminCommandTree DifficultyCommands RivalCommands SparCommands ProgressionCommands CharacterCommands MechanicsCommands; do
    if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/command/${class}.class" ]]; then
      mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/command"
      cp "$OUT/com/dbzlegacy/adaptivedifficulty/command/${class}.class" \
        "$merge/com/dbzlegacy/adaptivedifficulty/command/${class}.class"
      cp "$OUT/com/dbzlegacy/adaptivedifficulty/command/${class}\$"*.class \
        "$merge/com/dbzlegacy/adaptivedifficulty/command/" 2>/dev/null || true
    fi
  done
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/data/PlayerDataClear.class" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/data"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/data/PlayerDataClear.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/data/PlayerDataClear.class"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/data/PlayerDataClear\$"*.class \
      "$merge/com/dbzlegacy/adaptivedifficulty/data/" 2>/dev/null || true
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/character/CharacterServicesStore.class" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/character"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/character/CharacterServicesStore.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/character/CharacterServicesStore.class"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/character/CharacterServicesStore\$"*.class \
      "$merge/com/dbzlegacy/adaptivedifficulty/character/" 2>/dev/null || true
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/character/CharacterServicesSystem.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/character/CharacterServicesSystem.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/character/CharacterServicesSystem.class"
  fi
  for class in CharacterServicesAccess CharacterServicesPermissionBootstrap; do
    if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/character/${class}.class" ]]; then
      mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/character"
      cp "$OUT/com/dbzlegacy/adaptivedifficulty/character/${class}.class" \
        "$merge/com/dbzlegacy/adaptivedifficulty/character/${class}.class"
    fi
  done
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/gui/CharacterServicesGuiApi.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/gui/CharacterServicesGuiApi.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/gui/CharacterServicesGuiApi.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.class" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/event"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.class"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents\$"*.class \
      "$merge/com/dbzlegacy/adaptivedifficulty/event/" 2>/dev/null || true
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/util/StaffAccess.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/util/StaffAccess.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/util/StaffAccess.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/service/DifficultyActions.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/service/DifficultyActions.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/service/DifficultyActions.class"
  fi
  if [[ -d "$OUT/com/dbzlegacy/adaptivedifficulty/progression/end" ]]; then
    mkdir -p "$merge/com/dbzlegacy/adaptivedifficulty/progression/end"
    cp -a "$OUT/com/dbzlegacy/adaptivedifficulty/progression/end/." \
      "$merge/com/dbzlegacy/adaptivedifficulty/progression/end/"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/progression/race/AndroidConversion.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/progression/race/AndroidConversion.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/progression/race/AndroidConversion.class"
  fi
  if [[ -f "$OUT/com/dbzlegacy/adaptivedifficulty/progression/ProgressionModuleCatalog.class" ]]; then
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/progression/ProgressionModuleCatalog.class" \
      "$merge/com/dbzlegacy/adaptivedifficulty/progression/ProgressionModuleCatalog.class"
    cp "$OUT/com/dbzlegacy/adaptivedifficulty/progression/ProgressionModuleCatalog\$"*.class \
      "$merge/com/dbzlegacy/adaptivedifficulty/progression/" 2>/dev/null || true
  fi
  cp "$RES/META-INF/mods.toml" "$merge/META-INF/mods.toml"
  # Ki/stamina pool fixes live in the base jar bytecode — never replace mixin wiring from src.
  if [[ ! -f "$merge/legacymechanics.mixins.json" ]]; then
    cp "$RES/legacymechanics.mixins.json" "$merge/legacymechanics.mixins.json"
  fi
  (cd "$merge" && jar cfm "$dest" META-INF/MANIFEST.MF .)
  if ! unzip -p "$dest" legacymechanics.mixins.json | cmp -s - <(unzip -p "$base" legacymechanics.mixins.json); then
    echo "ERROR: merged jar mixins.json differs from base — ki/stamina wiring must stay on live base" >&2
    exit 1
  fi
  rm -rf "$tmp" "$merge"
}

if [[ -f "$BASE_JAR" ]]; then
  echo "Merging compiled CNPC overlay onto base: $(basename "$BASE_JAR")"
  merge_onto_base_jar "$BASE_JAR" "$JAR"
else
  echo "WARN: Base jar missing ($BASE_JAR) — full compile jar (no live ki/stamina base)" >&2
  (
    cd "$OUT"
    jar cvmf "$RES/META-INF/MANIFEST.MF" "$JAR" $(find com -name '*.class') \
      -C "$RES" META-INF/mods.toml \
      -C "$RES" pack.mcmeta \
      -C "$RES" legacymechanics.mixins.json \
      -C "$RES" legacymechanics.refmap.json
  )
fi

# Ki pool: do NOT overlay reference/ki-pool-2.4.115 .class
# files — mixing 2.4.115 bytecode with a fresh compile causes Java 17 VerifyError during
# StatsData#load (mixins → DmzResourcePoolClamp) and clients see "Invalid player data".

echo "Built $JAR (base: $(basename "$BASE_JAR"))"
jar tf "$JAR"

# Fail-closed audits: product features, combat scaling sim, GUI ABI.
HERE_SIM="$(cd "$(dirname "$0")" && pwd)/sim"
python3 "$HERE_SIM/audit_overhaul_scale_delegation.py"
python3 "$HERE_SIM/audit_cnpc_gui_style.py"
python3 "$HERE_SIM/audit_forge_gui_backend.py"
python3 "$HERE_SIM/audit_scaling_sync.py"
python3 "$HERE_SIM/audit_features.py"
python3 "$HERE_SIM/audit_command_humanization.py"
python3 "$HERE_SIM/audit_prestige_need_ladder.py"
python3 "$HERE_SIM/audit_prestige_gui_flow.py"
python3 "$HERE_SIM/audit_gui_menus_comprehensive.py"
python3 "$HERE_SIM/audit_gui_tp_copy.py"
python3 "$HERE_SIM/audit_ship_manifest.py"
python3 "$HERE_SIM/audit_branch_manifest_matrix.py"
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
