# Mohist “must die to melee” (DMZ combat)

## Symptom

On Mohist + DragonMineZ, **some players** deal **no melee damage** until they **die once**. Others are fine. KubeJS login sync / attack fallbacks were not reliable.

## Cause

DMZ melee is applied in `CombatAttackRequestC2S` by calling `ServerPlayer.attack` (`m_5706_`). Mohist’s Bukkit bridge on that method is flaky until the player entity is recreated by death. DMZ’s real damage rewrite happens later in `CombatEvent.onLivingHurt` via `getMeleeDamage()`.

## Fix (use this)

**Forge mixin mod** (not a KubeJS script):

`mods/dmz_mohist_melee_fix-1.0.0.jar`

It redirects DMZ’s `ServerPlayer.attack(...)` call on living targets to `LivingEntity.hurt(playerAttack, 1.0F)`. DMZ’s existing `LivingHurt` handler still converts that into full melee damage.

- No gamemode flicker  
- No teleport  
- No kill / Otherworld side effects  
- Does not touch `keepInventory`

### Install on main server

1. Download:  
   https://github.com/anthonycasio92-debug/DMZServer/blob/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-1.0.0.jar  
2. Put in `mods/`  
3. **Remove** any old `kubejs/server_scripts/mohist_melee_combat_fix.js`  
4. Restart the server (mixins do not hot-reload)

### Verify

On startup, log should include:

```text
[dmz_mohist_melee_fix] DMZ CombatAttackRequest LivingEntity hits use hurt() instead of Player.attack()
```

Also check mixin apply (no `@Redirect` failure for `CombatAttackRequestC2SMixin`).

Then join **without dying** and melee a mob — all players should deal damage.

## Source / rebuild

`tools/dmz-mohist-melee-fix/` — see `README.md` there.

## If it still fails

1. Confirm the jar is loaded and mixin applied  
2. Update Mohist, or leave Mohist (pure Forge / other hybrid)  
3. Binary-search damage/PvP plugins (WorldGuard, CMI, Fabled)
