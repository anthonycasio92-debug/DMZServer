# Mohist M1 brick — attribute collapse + raid teleport lock

## Cause

1. **Forge `ENTITY_REACH`** — DMZ range = `weaponRange + (reach - default)`. NaN → silent miss.
2. **`dragonminez:ki_damage`** (also `melee_damage` / `strike_damage`) — `StatsData.getSecondaryAttributeValue` returns raw `getValue()` with no NaN check, so NaN poisons `getMeleeDamage` / `getKiDamage` and LivingHurt amount until death rebuilds attributes.
3. **Stale `strikeLocked`** — `CombatAttackRequestC2S.handle` drops M1 while `Status.isStunned()` (`strikeLocked || knockedDown || stunEffect`).
4. **Shurui Raid Bosses** — arena start teleports with `ServerPlayer.teleportTo(ServerLevel,...)` (often same dimension → no dim-change unlock), then `DmzHooks.fullHeal` clears knockdown/stunEffect/`removeAllEffects` but **does not clear `strikeLocked`**.

## Fix v2.3.0 (attributes + unlock only — no damage redirects)

`mods/dmz_mohist_melee_fix-2.3.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.3.0.jar

- Repairs Forge `ENTITY_REACH` / `BLOCK_REACH`
- Repairs `dragonminez:ki_damage`, `melee_damage`, `strike_damage` when non-finite
- Clears stale `strikeLocked` (packet gate) without redirecting hurt/attack
- After server teleports + Shurui `fullHeal` / raid teleport/return: force-abort leftover strike maps, unlock, repair attributes, and follow up at +5/+20 ticks

### Install

1. Put **2.3.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart

### Verify

```text
[dmz_mohist_melee_fix] v2.3.0 repair reach/ki_damage + unlock after raid/teleport (no damage redirect)
[dmz_mohist_melee_fix] SELFTEST PASS ...
```
