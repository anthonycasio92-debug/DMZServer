# Mohist M1 brick — attribute collapse

## Cause

1. **Forge `ENTITY_REACH`** — DMZ range = `weaponRange + (reach - default)`. NaN → silent miss.
2. **`dragonminez:ki_damage`** (also `melee_damage` / `strike_damage`) — `StatsData.getSecondaryAttributeValue` returns raw `getValue()` with no NaN check, so NaN poisons `getMeleeDamage` / `getKiDamage` and LivingHurt amount until death rebuilds attributes.

## Fix v2.2.0 (attributes only — no damage redirects)

`mods/dmz_mohist_melee_fix-2.2.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.2.0.jar

- Repairs Forge `ENTITY_REACH` / `BLOCK_REACH`
- Repairs `dragonminez:ki_damage`, `melee_damage`, `strike_damage` when non-finite
- Clears stale `strikeLocked` (packet gate) without redirecting hurt/attack

### Install

1. Put **2.2.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart

### Verify

```text
[dmz_mohist_melee_fix] v2.2.0 repair ENTITY_REACH + dragonminez:ki_damage (no damage redirect)
[dmz_mohist_melee_fix] SELFTEST PASS ... kiAfter=0.0 ...
```
