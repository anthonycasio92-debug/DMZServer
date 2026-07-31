# Adaptive Difficulty audit (mod **1.7.26** / GUI **1.7.11**)

Audited Forge mod `tools/dmz-adaptive-difficulty` + Bukkit companion `tools/dmz-adaptive-difficulty-gui`.

## Verdict

Enderman / Warden combat gravity now uses the real DMZ `GravityDeviceManager` path from **player proximity** (not only when the mob’s AI target is the player). Mob kits acquire nearby players so abilities keep firing after blinks. GUI honors `guiBackend` on Forge reopen.

---

## Fixed in this pass (1.7.26 / 1.7.11)

| Severity | Issue | Fix |
|---|---|---|
| Critical | Enderman/Warden gravity only applied when mob AI target was already the player — teleporting Endermen almost never applied pressure | `CombatGravity` scans nearby Endermen/Wardens/Gravity-mutants every player tick; registers a real device zone + `GravityStateSync` |
| Critical | Gravity TTL used player `tickCount` inconsistently; zone AABB was tight | TTL uses world game time; zone inflate `(4,3,4)` |
| High | Kits early-returned with `target == null` so leap/ki/flare idled | Nearest-player acquire on Enderman/Warden/Zombie/Skeleton/Blaze/Ghast/Piglin/Hoglin kits |
| High | Adaptive AI only acquired targets at Enhanced+ | Awakened+ nearest-player lock; Enhanced+ still prefers weakest |
| High | Large blasts used `setupKiSmall` | `setupKiLargeBlast` with short cast |
| High | Forge reopen ignored `guiBackend=chest/chat` (always preferred CMI inventory) | Bridges call `openMenuRespectingConfig` |
| Medium | Forge `do` invalid amounts silently became +100 | Reject non-numeric up/down/buy/set args |
| Medium | Rewards GUI lore still showed linear formula | Updated to power-curve text |
| Medium (prior) | Kill rewards wrote `potentialunlock` | Removed in 1.7.25 — CNPC `Potential.js` owns it |
| Medium (prior) | TP hard-capped at ×3.5 | Uncapped power curve in 1.7.24 |

---

## Mob kit status (Awakened+)

| Mob | Abilities | Notes |
|---|---|---|
| **Enderman** | Gravity device pressure, Solar Flare, TP combos, chase | Gravity via proximity scan |
| **Warden** | Heavier gravity, leap, ki barrage/beam/large/wave, TP | Gravity via proximity scan |
| **Creeper** | Chase, ignite (`m_32312_`), scaled radius/fuse, death boom skip if already exploded | Fixed earlier |
| **Zombie / generic melee** | Dash, leap, rush, ground slam, berserk | Acquires nearest player |
| **Skeleton / ranged** | Ki blast → laser → beam → charged → barrage | Projectile replace on arrow join |
| **Blaze** | Burning barrage/blast, explosion blast | No per-tick ignite |
| **Ghast** | Large blast, burn beam, explosive wave | Fireball replace |
| **Piglin** | Leap, small blast, rush combo | |
| **Zombie Piglin** | Leap, ki barrage, pack swarm (players only) | |
| **Hoglin** | Aerial launch / smash | Enhanced+ |
| **Gravity mutation** | Extra gravity mult on any mutated hostiles near player | |

Hostile→hostile damage still cancelled (pack civil-war guard).

---

## Remaining / accepted

| Severity | Issue | Notes |
|---|---|---|
| Medium | `hardCapDifficulty == 1000000` migrates to 0 | Legacy cleanup |
| Medium | Dual `/difficulty` (Forge + Bukkit) | Mohist prefers Bukkit; watch double-fire |
| Medium | Bukkit `admin set` reflection bypasses Forge clamps | Staff-only |
| Low | High-tier titles (Transcendent–Zenith) not granted | Only Impossible/Divine/God (+ boss/elite) |
| Low | Dead hurt mixin (noop) | Forge `LivingHurtEvent` is live path |
| Low | Burning flag set when firing ki, not on projectile hit | Cosmetic/feel; damage still scales on hurt |

---

## What works

- Prestige/level theoretical max (Zenith 10M defaults)
- Inventory-only Lightman's payments; lower/reset free
- Steps `{1,5,25,100,1000,10000,100000}`
- Offense power curve (uncapped high mult); health flat + hard caps
- TP reward power curve (uncapped)
- Potential unlock left to CNPC script
- Melee attribute scale + event skip via `dmz_ad_attr_dmg`
- Area / team modes, elites, mutations, boss phases

---

## Install

1. `mods/dmz_adaptive_difficulty-1.7.26.jar`
2. `plugins/dmz_adaptive_difficulty_gui-1.7.11.jar` (+ CMILib / CMI)
3. Remove older adaptive-difficulty jars
4. Restart (`difficulty=hard`)
5. Fight **new** mobs after raising difficulty (spawn-cached)
