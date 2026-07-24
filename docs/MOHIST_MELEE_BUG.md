# Mohist M1: swing / no kill

## Production log findings (latest.log)

1. Server was still on **fix jar v2.6.0** (need **2.9.0**).
2. **Raid teleport mixin failed to apply** — `Object` vs `Region` descriptor mismatch, so
   Shurui arena teleports never ran our unlock.
3. **`CombatAttackRequestC2S: invalid entity id count 66`** — DMZ max is 64; oversize
   packets are **dropped entirely** (local swing can still show, server never attacks).
   Common in crowded spawn/raids.

## Fix v2.9.0

`mods/dmz_mohist_melee_fix-2.9.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.9.0.jar

- Clamp oversized M1 entity-id lists (keep first 64) instead of dropping the packet
- Fix Shurui `RaidInstance.teleport(ServerPlayer, Region)` mixin signature + hook `teleportToArena`
- Keep respawn-like recovery / STR snapshot / reach floor from 2.8.0

### Install

1. Only **2.9.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar` (especially 2.6.0)
3. Restart — log must show `v2.9.0`, **no** `RaidInstanceTeleportMixin` apply failure,
   and ideally no `invalid entity id count`
