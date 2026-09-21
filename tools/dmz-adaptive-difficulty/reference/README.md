# Live ki/stamina reference

`build.sh` no longer overlays pool-clamp bytes from recycle jars (mixed bytecode caused VerifyError). Reference class kept for manual diff only.

Ship line starts at **4.5.0** (`LegacyMechanics-4.5.0.jar` / matching GUI). Bump `AdaptiveDifficultyMod.VERSION`, `mods.toml`, and `plugin.yml` together before each release.

Generate locally:

```bash
unzip -p /path/to/recycle/LegacyMechanics-2.4.115.jar \
  com/dbzlegacy/adaptivedifficulty/progression/DmzResourcePoolClamp.class \
  > tools/dmz-adaptive-difficulty/reference/DmzResourcePoolClamp.class
```

Or set `LM_REFERENCE_JAR` to that recycle jar before `build.sh` (auto-extract if this file is missing).
