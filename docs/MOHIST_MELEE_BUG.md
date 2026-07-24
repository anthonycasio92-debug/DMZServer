# Mohist “must die to melee” (DMZ combat)

## Symptom

M1 deals no damage until the player dies. Triggers include restart/rejoin, cancelled ki blasts, cancelled strike techniques, and PvP attempts in no-PvP regions.

## Root cause

1. **Forge `ENTITY_REACH` corruption** — DMZ computes hit range as `weaponRange + (entityReach - default)`. If reach base/modifiers go NaN or collapse on Mohist, server range checks fail while the client still targets. Death rebuilds the AttributeMap.
2. **Stale `strikeLocked`** — DMZ drops CombatAttackRequest when `Status.isStunned()` (includes `strikeLocked`).
3. **Client charge/block desync** — cancelled ki clears charge on the server without syncing; client `MinecraftMixin` keeps cancelling M1.
4. **Stuck client upswing** / Mohist CraftPlayer state until respawn.

## Fix v1.0.6

`mods/dmz_mohist_melee_fix-1.0.6.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-1.0.6.jar

- Repairs Forge `ENTITY_REACH` / `BLOCK_REACH` (NaN/collapsed) and sanitizes `getEffectiveAttackRange`
- Clears locks + charge/block flags and syncs to client
- Client upswing flush + soft player recreate on Mohist join
- Bukkit-bypass damage for melee / ki / strike

### Install

1. Put **1.0.6** in `mods/` (server **and** clients)
2. Delete older `dmz_mohist_melee_fix-1.0.*.jar`
3. Restart

### Verify

```text
[dmz_mohist_melee_fix] v1.0.6 repair Forge ENTITY_REACH + client sync + soft refresh
[dmz_mohist_melee_fix] SELFTEST PASS ... reachAfter=... fixedRange=...
```

## Source

`tools/dmz-mohist-melee-fix/`
