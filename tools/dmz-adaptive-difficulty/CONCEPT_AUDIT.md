# Concept audit vs `DragonMineZ_Adaptive_Difficulty_System_Concept_019c.txt`

Mod version: **1.6.2** (server-side only)  
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
| 11 | Enemy evolution (creeper/zombie/skel/enderman/warden) | Done* | `EnemyEvolution` — creeper larger blast/faster fuse via radius+swell; ki beams are stand-ins |
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
- **Skeleton “ki beam/laser”**: effect/damage stand-ins (not full DMZ ki projectiles)
- **DMZ XP**: vanilla XP points (no dedicated DMZ XP gain event in 2.1.3)
- **Cosmetics**: capsule item drops (not wardrobe skins); titles unlock + Statistics list
- **Admin command shape**: nested under `/difficulty admin` with toggle (ops / `difficulty.admin`)
- **Phys vs Ki DMZ extras**: folded into shared outgoing damage multiplier for mobs

## v1.6.2 reaudit fixes
- Anti-flight now disables DMZ `fly` skill + removes `MainEffects.FLY` (not only creative fly)
- Red / Extreme state is reachable (full team ceiling or hard-cap band) and shown in GUIs
- Unlocked titles listed on Statistics (chat / CMI / chest)
- Ability unlock tier thresholds admin-editable (`tierAwakened` … `tierImpossible`)
- Admin set covers movement % and `dmzExtra*` keys
- Creepers gain larger `explosionRadius` + faster fuse by tier
