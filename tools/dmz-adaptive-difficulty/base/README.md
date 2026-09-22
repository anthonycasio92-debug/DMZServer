# Forge mod working base (live)

**Do not reimplement ki/stamina overflow fixes in isolation.** Builds merge CNPC/GUI patches from `src/` onto this jar.

| File | Source |
|------|--------|
| `LegacyMechanics-4.5.23-direct-dmz-resource-max.jar` | Live `mods/` (ki + stamina overflow fixes) |

Refresh from production:

```bash
bash scripts/pull-lm-base-jar.sh
```

`build.sh` uses this when present (`LM_BASE_JAR` override optional). Compiled overlay packages default to `gui/cnpc/` plus `RivalGuiApi`. **Do not** overlay `legacymechanics.mixins.json` from source — the base jar omits `StatsDataHudPoolMaxMixin`; re-adding it double-scales ki/stamina.
