# Mohist M1: animation but no damage

## Fix v2.12.0-fixed

`mods/dmz_mohist_melee_fix-2.12.0-fixed.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.12.0-fixed.jar

Built on the reviewed **2.11.0-fixed** optimized rewrite (`CombatRepair` / `RepairEvents` / `MeleeRescue`), plus the **2.12** intentional DMZ stat-reset changes.

### Stat reset fix
Primary snapshot restore was treating intentional DMZ resets (`resetPlayerProgress` /
`setStrength(0)`) like Mohist dim-wipes and putting old STR/etc back within seconds.

- Record intentional `Stats.setAttributeBaseValue` writes into the snapshot (including 0)
- On `resetPlayerProgress`: clear snapshot, suppress restore briefly, adopt post-reset values
- Read-side STR fallback / tick ensure skip while suppress is active
- Periodic snapshot skips while suppress is active (avoids re-capturing wiped zeros incorrectly after reset)

### Still included (2.11.0-fixed)
- Empty/stale entity-ID M1 rescue with AABB fall-through
- Clamp >64 entity ID packets
- Reach / primary sanitize + respawn-like recovery
- No raid-mixin coupling or live selftests (lean fixed rewrite)

### Install

1. Only **2.12.0-fixed** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar` (including `2.11.0-fixed` and plain `2.12.0`)
3. Restart — log: `v2.12.0-fixed`
