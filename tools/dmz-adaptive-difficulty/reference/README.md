# Live ki/stamina reference

`build.sh` no longer overlays pool-clamp bytes from recycle jars (mixed bytecode caused VerifyError). Reference class kept for manual diff only.

Ship line starts at **4.5.0** (`LegacyMechanics-4.5.0.jar` / matching GUI). Bump `AdaptiveDifficultyMod.VERSION`, `mods.toml`, and `plugin.yml` together before each release.

**Ki/stamina:** `DmzResourcePoolClamp.actualMaxEnergy/Stamina` must call private `actualMax(...)` (native read via `isReadingNativeMax`, Iron reject, HUD formula fallback, then × Overhaul scale once). Do **not** return raw `data.getMaxEnergy()` — that regressed in the 2.4.115 overlay (4.5.x before 4.5.4) and breaks clamps/regen/HUD alignment. Good reference commit: `51afb1aa`.

**HUD max getters:** `StatsDataHudPoolMaxMixin` must call `canonicalPoolMax(data, vanillaReturn, energy)` on the vanilla RETURN value — never `actualMaxEnergy()` (re-enters `getMaxEnergy`). Clamps use `actualMax*` → `readNativeMax` + same `canonicalMax` math. When vanilla max is stub-low vs HUD formula, `mergeNativeWithHudFormula` lifts to the formula (Mohist secondary attr bug).

Generate locally:

```bash
unzip -p /path/to/recycle/LegacyMechanics-2.4.115.jar \
  com/dbzlegacy/adaptivedifficulty/progression/DmzResourcePoolClamp.class \
  > tools/dmz-adaptive-difficulty/reference/DmzResourcePoolClamp.class
```

Or set `LM_REFERENCE_JAR` to that recycle jar before `build.sh` (auto-extract if this file is missing).
