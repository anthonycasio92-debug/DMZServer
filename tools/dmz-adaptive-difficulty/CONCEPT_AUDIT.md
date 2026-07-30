# Concept audit vs `DragonMineZ_Adaptive_Difficulty_System_Concept_019c.txt`

Mod version: **1.7.9** (server-side only; CMI GUI, no DeluxeMenus)  
Source concept: `tools/dmz-adaptive-difficulty/DragonMineZ_Adaptive_Difficulty_System_Concept_019c.txt`

Reaudited against live Java sources (not prior audit claims).

| § | Concept | Status | Implementation |
|---|---------|--------|----------------|
| 1 | System overview | Done | Framing realized by the stack below |
| 2 | Optional / player-controlled + Lightman's + team | Done | Active selection, purchases, team modes |
| 3 | Calculated / Purchased / Active / Team Threshold / Contribution | Done | `DifficultyCalculator`, `PlayerDifficultyData`, `TeamScaling`, `DmzProgression` |
| 4 | Personal / Threshold / Full team modes (+10%/teammate) | Done | `TeamMode` + FTB Teams (scoreboard fallback) |
| 5 | `/difficulty` GUI fields + buttons | Done | CMI → chest → chat; Increase/Decrease/Team/Rewards/Tiers/Stats (+ Buy) |
| 6 | Lightman's +100/+1k/+10k; lowering free | Done | `CurrencyBridge` wallet+bank+inventory; TP/`free` fallback |
| 7 | Admin config / `difficulty.admin` / ability unlock tiers | Done | `/difficulty admin set …` incl. scaling keys + `tierAwakened`…`tierImpossible` |
| 8 | State colors G/Y/O/P/R | Done | `DifficultySnapshot.state()`; Extreme = full team ceiling or hard-cap; shown in GUIs |
| 9 | Mob spawn scaling + DMZ extras | Done | `MobScaling` + `AreaDifficulty` (SH-style); DMZ extras on DMZ mobs |
| 10 | Tiers 10→100000 | Done | `DifficultyTier` defaults match concept; thresholds config-driven |
| 11 | Enemy evolution (creeper/zombie/skel/enderman/warden) | Done | `EnemyEvolution` — all hostiles; special kits + shared melee/ranged (ki) packages; Stray/WitherSkeleton included |
| 12 | Elites (name/aura/size/AI/rewards) | Done* | `EliteSystem` — glow aura + knockback-resist size + NBT scale hint (no Pehkui) |
| 13 | Mutations listed | Done | All five + Shadow / Vampiric extras |
| 14 | Adaptive AI incl. ki-charge + anti-flight | Done | Dodge/retreat/ki-charge/`MainEffects.FLY`+fly skill/focus/coord |
| 15 | Rewards TP/XP/Potential/drops/capsules/titles | Done* | Kill package + TP event; titles listed in Statistics; XP = vanilla points |
| 16 | Boss scale + phases | Done* | `BossScaling` HP/armor/name + 75/50/25% phases |
| 17 | Cache / no per-tick calc | Done | `DifficultyCache` + mob NBT; level/prestige polled every 5s |

\* = intentional approximation noted below (still functionally present).

## Intentional approximations (not blockers)
- **GUI host**: CMILib/CMI inventory / Bukkit chest / chat (no Forge client Screen jar)
- **Elite size / aura**: no Pehkui; knockback resist + glowing name; NBT `dmz_ad_elite_scale`
- **DMZ XP**: vanilla XP points (no dedicated DMZ XP gain event in 2.1.3)
- **Cosmetics**: capsule item drops (not wardrobe skins); titles unlock + Statistics list
- **Admin command shape**: nested under `/difficulty admin` with toggle (ops / `difficulty.admin`)
- **Phys vs Ki DMZ extras**: folded into shared outgoing damage multiplier for mobs

## Mob power ladder (concept §9–§14) — how to see ki
Mobs scale once at spawn from nearby **active** difficulty (area mode). Evolution/AI unlock by tier:

| Active difficulty | Tier | What you should notice |
|---|---|---|
| 0 | — | Stats only if somehow tagged; no evolution |
| 10+ | Awakened | Evolution starts (all hostiles) |
| **50+** | **Enhanced** | Glow + tier name; melee leap; ranged **Ki Blasts** |
| **100+** | **Elite** | Rush / **Lasers** / Warden barrage |
| **500+** | **Advanced** | Slam / **Beams**; anti-flight / dodge AI |
| **1000+** | **Master** | Berserk / **Charged Beams**; warden teleport |

Raise active difficulty via `/difficulty` → **Adjust**, then fight **newly spawned** hostiles (already-spawned mobs keep their old cached difficulty).

## v1.7.9 — evolution on all hostiles
- Was limited to Creeper/Zombie/Skeleton/Enderman/Warden (missed Stray/Wither Skeleton)
- Now every `MONSTER` / `Enemy` evolves (special kits + shared melee/ranged packages)
- Enhanced+ mobs briefly glow and get a tier nameplate

## v1.7.8 — real DMZ ki projectiles
- Replaced skeleton/warden effect stand-ins with `KiBlastEntity` / `KiLaserEntity` / `KiWaveEntity`
- Homing aimed shots; damage scales with tier + mob difficulty

## v1.6.2 reaudit fixes
- Anti-flight now disables DMZ `fly` skill + removes `MainEffects.FLY` (not only creative fly)
- Red / Extreme state is reachable (full team ceiling or hard-cap band) and shown in GUIs
- Unlocked titles listed on Statistics (chat / CMI / chest)
- Ability unlock tier thresholds admin-editable (`tierAwakened` … `tierImpossible`)
- Admin set covers movement % and `dmzExtra*` keys
- Creepers gain larger `explosionRadius` + faster fuse by tier

## v1.7.0 optimization + CMI-only GUI
- Removed DeluxeMenus bridge, configs, and jar; legacy `guiBackend=deluxemenus` remaps to `cmi`
- LivingTick: Mob-only, unmarked scaled-zero exit, sparse spawn retry, staggered AI/evolution
- Area difficulty uses cached player snapshots + 250ms chunk TTL (no full refresh on spawn)
- PersistentDataAccess MethodHandle; cached damage multiplier on mob NBT
- Logout no longer invalidates every player's cache
- ForgeBridge caches reflective handles + 200ms placeholder map
