# Mohist “must die to melee” (DMZ combat)

## Symptom

On Mohist hybrid (`1.20.1-46ca7304` / Forge 47.4.x + DragonMineZ 2.1.3), **some players** deal **no melee damage** until they **die and respawn once**. Others are fine. Soft “attribute refresh” alone does not fix everyone.

## Why it happens

DragonMineZ melee path:

1. Client cancels vanilla `startAttack`
2. Client sends `CombatAttackRequestC2S`
3. Server calls `ServerPlayer.attack(target)`
4. `CombatEvent.onLivingHurt` rewrites damage to `getMeleeDamage()`

Mohist patches `Player.attack` / damage bridging. For affected players the attack starts (or is accepted) but **damage never lands** until the `ServerPlayer` is recreated by death. Login sync timing also varies, so the bug is **intermittent per player**.

Server log evidence:

```text
Injection warning: LVT in ...PlayerList::respawn... has incompatible changes
  dragonminez.mixins.json:common.PlayerListMixin->...onPlayerRespawn
This server is running Mohist version 1.20.1-46ca7304 ... Forge 47.4.13
```

## What we will not do

- Gamemode flicker / micro-teleport
- Toggle `keepInventory`
- Auto-kill on login (unsafe: DMZ Otherworld treats death as story state)

## Workaround (`kubejs/server_scripts/mohist_melee_combat_fix.js`)

1. **Login sync (~1.25s):** clear DMZ stun/block/knockdown locks, `refreshAttributes`, re-send `StatsSyncS2C` + weapon registry  
2. **Attack fallback:** on `AttackEntityEvent`, wait 2 ticks — if `LivingHurt` did not apply damage / HP unchanged, call `hurt()` with DMZ melee damage (bypasses the broken `Player.attack` Bukkit bridge)

Watch console for:

```text
[mohist-melee-fix] Login sync for <name> (caps ok)
[mohist-melee-fix] Forced melee for <name> → #<id>
```

If you never see `Forced melee` for a broken player, Mohist is dropping the attack before `AttackEntityEvent` — then only a Mohist update / non-Mohist host fully fixes it.

## Real fixes (preferred)

1. Newer Mohist 1.20.1 build, or leave Mohist (pure Forge / other hybrid)  
2. Binary-search damage/PvP plugins (WorldGuard, CMI, Fabled, etc.)  
3. Minimal mod set test: DMZ + GeckoLib + TerraBlender + Curios only
