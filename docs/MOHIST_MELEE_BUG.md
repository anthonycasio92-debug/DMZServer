# Mohist M1: animation but no damage

## Fix v2.12.1

`mods/dmz_mohist_melee_fix-2.12.1.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.12.1.jar

Optimized layout from the reviewed 2.11.0-fixed cleanup, with hardened intentional DMZ stat-reset handling.

### Cleanup kept from fixed jar
- Combined classes: `CombatRepair`, `RepairEvents`, `MeleeRescue`, `RateLog`
- Removed raid mixin layer, `PersistentDataAccess`, zombie-spawn self-tests
- Dropped redundant `PlayerChangedDimensionEvent` (teleport/dim mixins cover it)
- Teleport follow-ups trimmed to `5, 15, 40`

### Stat reset fix (hardened)
- Record intentional primary writes (public setters + `setAttributeBaseValue`), including 0
- On `resetPlayerProgress`: cancel delayed repairs, clear snapshot, suppress restore (~10s)
- Full resets force a zero snapshot; percentage resets adopt live post-reset values
- Read-side STR fallback / tick ensure skip while suppress is active

### Install

1. Only **2.12.1** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — log: `v2.12.1` + `RESET SELFTEST PASS`
