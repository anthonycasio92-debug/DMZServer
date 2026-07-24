# Mohist “must die to melee” (DMZ combat)

## Symptom

On Mohist + DragonMineZ, **some players** deal **no melee damage** until they **die once**. Others are fine. A restart can break a player who was working before. KubeJS workarounds and calling `hurt`/`actuallyHurt` alone were not enough — Mohist can still route those through a broken Bukkit `EntityDamageEvent` bridge.

## Cause

DMZ melee is applied in `CombatAttackRequestC2S` by calling `ServerPlayer.attack` (`m_5706_`). Mohist’s Bukkit bridge on attack/hurt is flaky until the player entity is recreated by death. DMZ’s real damage rewrite happens in `CombatEvent.onLivingHurt` via `getMeleeDamage()`.

Also: DMZ only processes `CombatAttackRequest` when `StatsCapability` is present; if that capability is missing on join, packets are silently ignored until death recreates the player.

## Fix (use this)

**Forge mixin mod** (server-side only):

`mods/dmz_mohist_melee_fix-1.0.2.jar`

Redirects DMZ’s `ServerPlayer.attack(...)` on living targets to:

1. Forge `LivingHurtEvent` once (DMZ rewrites to `getMeleeDamage()`)
2. Direct `setHealth` / `die` — **never** calls `attack`, `hurt`, or `actuallyHurt`

Also logs join-time `StatsCapability` presence and the first few melee hits per player.

- No gamemode flicker  
- No teleport  
- No kill / Otherworld side effects  
- Does not touch `keepInventory`

### Install on main server

1. Download:  
   https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-1.0.2.jar  
2. Put in `mods/`  
3. **Delete** any older `dmz_mohist_melee_fix-1.0.0.jar` / `1.0.1.jar`  
4. Restart the server (mixins do not hot-reload)

### Verify (required)

After restart, logs **must** contain:

```text
[dmz_mohist_melee_fix] v1.0.2 CombatAttackRequest → LivingHurt + setHealth (full Bukkit bypass)
Selecting config dmz_mohist_melee_fix.mixins.json
Mixing CombatAttackRequestC2SMixin ... into CombatAttackRequestC2S
```

On player join:

```text
[dmz_mohist_melee_fix] join probe player=NAME statsCapability=true
```

If `statsCapability=false`, CombatAttackRequest is ignored by DMZ — that is a separate join/capability bug.

When the affected player M1s a mob, logs should show:

```text
[dmz_mohist_melee_fix] hit player=NAME target=... delta=... applied amount=...
```

- If **no hit lines** appear while M1 animates: packet never reached the redirect (slot mismatch / hand null / capability / client not sending).  
- If hit lines show `delta=0` / `*Cancelled`: another Forge/plugin cancelled the event after our redirect.  
- If hit lines show `delta>0` but client sees no damage: sync/display issue, not application.

## Source / rebuild

`tools/dmz-mohist-melee-fix/` — see `README.md` there.

## If it still fails

1. Confirm **v1.0.2** + mixin apply + join probe + hit lines above  
2. Only one `dmz_mohist_melee_fix-*.jar` in `mods/`  
3. Paste those log lines for the broken player  
4. Update Mohist or leave Mohist (pure Forge / other hybrid)  
5. Binary-search damage/PvP plugins (WorldGuard, CMI, Fabled)
