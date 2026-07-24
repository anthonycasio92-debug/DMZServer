# Mohist “must die to melee” (DMZ combat)

## Symptom

M1 deals no damage until the player dies. Triggers include restart/rejoin, cancelled ki blasts, cancelled strike techniques, and PvP attempts in no-PvP regions.

## Root cause (confirmed on Mohist)

1. **Stale `strikeLocked`** — DMZ `CombatAttackRequest.handle` ignores packets when `Status.isStunned()` is true, and that includes `strikeLocked`. Cancelled strike/PvP can leave `strikeLocked=true`, so every M1 packet is silently dropped until death/login clears it.
2. **Mohist Bukkit damage bridge** — `Player.attack` / `LivingEntity.hurt` are flaky after cancelled EntityDamage events.

## Fix v1.0.4 (server-side)

`mods/dmz_mohist_melee_fix-1.0.4.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-1.0.4.jar

- Unlocks stale `strikeLocked` on every M1 packet (only real STUN potion still blocks)
- Bukkit probe respects WorldGuard no-PvP without using broken hurt/attack
- Applies allowed damage via LivingHurt + setHealth
- Same bypass/repair for melee, ki blasts, and strike techniques
- Auto self-test on Mohist boot

### Install

1. Put **1.0.4** in `mods/`
2. Delete older `dmz_mohist_melee_fix-1.0.*.jar`
3. Restart

### Verify (Mohist logs)

```text
[dmz_mohist_melee_fix] v1.0.4 unlock stale strikeLocked + melee/ki/strike Bukkit-bypass
[dmz_mohist_melee_fix] Self-test enabled (mohist=true forced=false)
[dmz_mohist_melee_fix] SELFTEST PASS lockedBefore=true lockedAfter=false ... delta=1.0
```

### Tested in this environment

Local Mohist 1.20.1 + DMZ 2.1.3:

- Mixins applied (CombatAttackRequest handle+redirect, ki, strike)
- SELFTEST **PASS**: simulated `strikeLocked=true` → cleared → zombie HP 20→19

## Source

`tools/dmz-mohist-melee-fix/`
