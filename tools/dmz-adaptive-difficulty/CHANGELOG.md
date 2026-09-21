# LegacyMechanics (Forge) + LegacyMechanicsGUI

All notable changes to the **2.5** product line are documented here.

## 2.5 — 2026-09-21

### Level cap

- Personal playable cap is **100k + 10k×level breakthroughs** (max **150k**), independent of held prestige.
- Overhaul’s native **+5k per held prestige** ladder is overridden on the server (mixins priority **2000**).
- Boot/login pin: `LevelingRevamp.json` **100k / 150k** via `pinOverhaulLevelCaps()`.
- Breakthrough purchases use full `setBreakthroughs` NBT; cap publish pushes **DMZ `StatsSyncS2C`** (no KubeJS client shim).

### Versioning

- First **2.5** release: matching **`LegacyMechanics-2.5.jar`** + **`LegacyMechanicsGUI-2.5.jar`** (`AdaptiveDifficultyMod.VERSION` = `plugin.yml` version).
