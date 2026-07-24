# Mohist “must die to melee” — ENTITY_REACH collapse

## Symptom

M1 deals no damage until the player dies/respawns.

## Cause (this fix)

DMZ computes hit range as:

```text
weaponRange + (Forge ENTITY_REACH - default)
```

On Mohist, `ENTITY_REACH` can collapse (NaN / invalid base). Server range checks then fail while the client still targets. Death rebuilds the AttributeMap, which is why suicide “fixed” it.

## Fix v2.0.0 (reach only)

`mods/dmz_mohist_melee_fix-2.0.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.0.0.jar

**Only** repairs Forge `ENTITY_REACH` / `BLOCK_REACH` and sanitizes DMZ `getEffectiveAttackRange`.

Removed (broke NPC kills in 1.0.x):

- DamageBridge / LivingHurt+setHealth redirects
- CombatAttackRequest / ki / strike mixins
- Soft player recreate
- Client upswing / charge-sync combat hooks

### Install

1. Put **2.0.0** in `mods/`
2. Delete **all** older `dmz_mohist_melee_fix-1.*.jar`
3. Restart

### Verify

```text
[dmz_mohist_melee_fix] v2.0.0 ENTITY_REACH collapse repair only
[dmz_mohist_melee_fix] SELFTEST PASS reachBefore=... reachAfter=3.0 sanitizedRange=2.0
```

## Source

`tools/dmz-mohist-melee-fix/`
