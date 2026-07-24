# Mohist “must die to melee” (DMZ combat)

## Symptom

M1 deals no damage until the player dies. Triggers include restart/rejoin, cancelled ki blasts, cancelled strike techniques, and PvP attempts in no-PvP regions.

## Root cause

1. **Stale `strikeLocked`** — DMZ drops CombatAttackRequest when `Status.isStunned()` (includes `strikeLocked`).
2. **Client charge/block desync** — cancelled ki clears charge on the server but without `StatsSyncS2C` the client still thinks `isChargingTechnique`/`isBlocking`, so `MinecraftMixin.startAttack` cancels all M1 until death recreates the player.
3. **Stuck client upswing** — `attackCooldown=10000` / upswing flags on `Minecraft` can stick when the local player is briefly null.
4. **Mohist Bukkit damage bridge** — `Player.attack` / `LivingEntity.hurt` stay flaky after cancelled EntityDamage events until the player entity is recreated.

## Fix v1.0.5 (server + client)

`mods/dmz_mohist_melee_fix-1.0.5.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-1.0.5.jar

- Clears stale locks + technique charge / stuck ki-charge / block flags, then **syncs stats to the client**
- Client mixin + login hooks cancel stuck DMZ upswing (no more relying on death)
- Soft player recreate once on Mohist join (and after repeated denied hits) — same recovery as suicide, without dying or enabling `keepInventory`
- Bukkit probe + LivingHurt + setHealth for melee / ki / strike

### Install

1. Put **1.0.5** in `mods/` (clients need it too for the upswing flush)
2. Delete older `dmz_mohist_melee_fix-1.0.*.jar`
3. Restart

### Verify (Mohist logs)

```text
[dmz_mohist_melee_fix] v1.0.5 client sync + soft refresh (no-suicide recovery)
[dmz_mohist_melee_fix] Self-test enabled (mohist=true forced=false)
[dmz_mohist_melee_fix] SELFTEST PASS lockedBefore=true lockedAfter=false ... delta=1.0
[dmz_mohist_melee_fix] soft player refresh player=... reason=join
```

## Source

`tools/dmz-mohist-melee-fix/`
