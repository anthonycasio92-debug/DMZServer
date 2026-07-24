# Mohist M1 brick — reach collapse + raid teleport lock

## Cause

1. **Forge `ENTITY_REACH`** — DMZ range = `weaponRange + (reach - default)`. NaN → silent miss.
2. **Stale `strikeLocked`** — `CombatAttackRequestC2S.handle` drops M1 while `Status.isStunned()`.
3. **Shurui Raid Bosses** — arena teleport + `DmzHooks.fullHeal` leaves `strikeLocked` set.
4. **NaN secondary attrs** — `StatsData.getSecondaryAttributeValue` returns raw `getValue()` with no NaN check (poisoned hurt amounts).

## Fix v2.4.0

`mods/dmz_mohist_melee_fix-2.4.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.4.0.jar

- Repairs Forge `ENTITY_REACH` / `BLOCK_REACH` only (never wipes all modifiers)
- **Does not write** `dragonminez:ki_damage` / `melee_damage` / `strike_damage` (v2.2–2.3 could reset those to 0 / strip gear mods)
- NaN on DMZ secondaries is guarded **on read** via mixin
- Clears stale `strikeLocked` after teleports / Shurui `fullHeal`
- No damage redirects

### Install

1. Put **2.4.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart

### Verify

```text
[dmz_mohist_melee_fix] v2.4.0 reach repair + raid unlock; no DMZ damage-attr writes
[dmz_mohist_melee_fix] SELFTEST PASS ... kiPreserved=true ...
```
