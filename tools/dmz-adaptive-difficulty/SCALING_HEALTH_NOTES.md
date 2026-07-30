# Scaling Health as design reference

Source studied: [SilentChaos512/ScalingHealth](https://github.com/SilentChaos512/ScalingHealth) (`1.20` branch), CurseForge overview.

## Patterns we adopted (server-side only)

| Scaling Health | DMZ Adaptive Difficulty |
|---|---|
| Area difficulty from nearby players | `AreaDifficulty` (`weighted` / `average` / `max`) |
| Distance-weighted average (default) | `areaDifficultyMode=weighted` |
| Group bonus for multiple nearby players | `areaGroupBonusPercent` |
| ~0.95–1.05 difficulty variance per mob | `areaDifficultyVariancePercent` |
| Process mob a few ticks after spawn if needed | `LivingTick` retry while `tickCount` 3–39 |
| `sh_difficulty get` readout | `/difficulty admin area` |
| Blights / elites from difficulty | Existing elite / mutation systems |

## Patterns we did **not** copy

- Client HUD difficulty meter / heart overlays (we stay server-only; no client jar)
- Heart/power crystals for player max HP
- Datapack mechanics codec system (we keep JSON config)
- Command name `sh_difficulty` — we keep player UX on `/difficulty` via Bukkit/CMI

## Useful SH files

- `utils/mode/AreaDifficultyModes.java` — average / weighted / extrema / distance
- `capability/DifficultyAffectedCapability.java` — deferred `tick` processing
- `utils/MobDifficultyHandler.java` — blight chance + HP/damage from difficulty
- `command/DifficultyCommand.java` — op get/set/add for player & server difficulty
