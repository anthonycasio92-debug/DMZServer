# Forge mod working base (consolidated 4.5.49+)

**Do not reimplement ki/stamina overflow fixes in isolation.** Builds merge CNPC/GUI patches from `src/` onto this jar.

| File | Role |
|------|------|
| **`LegacyMechanics-4.5.49-consolidated.jar`** | Canonical base (ki/stamina + CNPC line deployed 2026-09-22) |
| `LegacyMechanics-4.5.23-direct-dmz-resource-max.jar` | Legacy fallback only if consolidated base missing |

`build.sh` keeps the base jar's **`legacymechanics.mixins.json`** unchanged (production omits `StatsDataHudPoolMaxMixin` from mixins config). Pool clamp bytecode stays from the base; overlay packages: `gui/cnpc/`, `RivalGuiApi`, `ProgressionGuiApi`, `AdaptiveDifficultyMod`, `AndroidConversion`.

Refresh from live after a good deploy:

```bash
bash scripts/pull-lm-base-jar.sh
# or copy last good build:
bash scripts/refresh-lm-consolidated-base.sh
```

Output artifact: **`mods/LegacyMechanics-${VERSION}.jar`** (not the old `direct-dmz-resource-max` filename).
