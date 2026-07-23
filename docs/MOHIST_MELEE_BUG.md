# Mohist “must die to melee” (DMZ combat)

## Symptom

On Mohist + DragonMineZ, **some players** deal **no melee damage** until they **die once**. Others are fine. KubeJS login sync / attack fallbacks were not reliable. Calling `LivingEntity.hurt` alone is also not enough — Mohist often routes that through the same broken Bukkit `EntityDamageEvent` bridge.

## Cause

DMZ melee is applied in `CombatAttackRequestC2S` by calling `ServerPlayer.attack` (`m_5706_`). Mohist’s Bukkit bridge on attack/hurt is flaky until the player entity is recreated by death. DMZ’s real damage rewrite happens later in `CombatEvent.onLivingHurt` via `getMeleeDamage()`.

## Fix (use this)

**Forge mixin mod** (server-side only):

`mods/dmz_mohist_melee_fix-1.0.1.jar`

It redirects DMZ’s `ServerPlayer.attack(...)` on living targets to:

1. Forge `LivingHurtEvent` (so DMZ still rewrites to `getMeleeDamage()`)
2. `LivingEntity.actuallyHurt` (`m_6475_`) — **bypasses** Mohist’s Bukkit `EntityDamageEvent` bridge

- No gamemode flicker  
- No teleport  
- No kill / Otherworld side effects  
- Does not touch `keepInventory`

### Install on main server

1. Download:  
   https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-1.0.1.jar  
2. Put in `mods/`  
3. **Delete** any older `dmz_mohist_melee_fix-1.0.0.jar`  
4. **Remove** any old `kubejs/server_scripts/mohist_melee_combat_fix.js`  
5. Restart the server (mixins do not hot-reload)

### Verify (required)

After restart, `logs/latest.log` / `debug.log` **must** contain:

```text
[dmz_mohist_melee_fix] v1.0.1 CombatAttackRequest uses Forge LivingHurt + actuallyHurt (Bukkit bypass)
```

and:

```text
Selecting config dmz_mohist_melee_fix.mixins.json
Mixing CombatAttackRequestC2SMixin from dmz_mohist_melee_fix.mixins.json into com.dragonminez.common.network.C2S.CombatAttackRequestC2S
```

If those lines are missing, the jar is not loading (wrong folder, not restarted, or duplicate/corrupt jar).

On the first successful melee after install you should also see:

```text
[dmz_mohist_melee_fix] Melee redirect active: Forge LivingHurt + actuallyHurt ...
```

Then join **without dying** and melee a mob.

Optional local probe: `-Ddmz.melee.fix.selftest=true` → `SELFTEST PASS ...`

## Source / rebuild

`tools/dmz-mohist-melee-fix/` — see `README.md` there.

## If it still fails

1. Confirm the **v1.0.1** log line and mixin apply lines above  
2. Confirm only one `dmz_mohist_melee_fix-*.jar` is in `mods/`  
3. Update Mohist, or leave Mohist (pure Forge / other hybrid)  
4. Binary-search damage/PvP plugins (WorldGuard, CMI, Fabled)
