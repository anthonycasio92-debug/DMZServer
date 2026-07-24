# Mohist M1: animation but no damage

## Fix v2.12.0

`mods/dmz_mohist_melee_fix-2.12.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.12.0.jar

### Stat reset fix
Primary snapshot restore was treating intentional DMZ resets (`resetPlayerProgress` /
`setStrength(0)`) like Mohist dim-wipes and putting old STR/etc back within seconds.

- Record intentional `Stats.setAttributeBaseValue` writes into the snapshot (including 0)
- On `resetPlayerProgress`: clear snapshot, suppress restore briefly, adopt post-reset values
- Read-side STR fallback / tick ensure skip while suppress is active

### Still included (2.11)
- Empty/stale entity-ID M1 rescue with AABB fall-through
- Clamp >64 entity ID packets, Shurui raid teleport mixin, respawn-like recovery
- Live `FALLBACK SELFTEST` + new `RESET SELFTEST`

### Install

1. Only **2.12.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — log: `v2.12.0` + `SELFTEST PASS` + `FALLBACK SELFTEST PASS` + `RESET SELFTEST PASS`
