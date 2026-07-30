# Concept audit vs `DragonMineZ_Adaptive_Difficulty_System_Concept_019c.txt`

Mod version: **1.3.2**

| § | Concept | Status | Notes |
|---|---------|--------|-------|
| 3 | Calculated / Purchased / Active / Team Threshold / Contribution | Done | Prestige 0 = level; 1+ = level × (prestige × mult) |
| 4 | Team modes Personal / Threshold / Full | Done | Vanilla scoreboard teams; +10%/teammate |
| 5 | `/difficulty` GUI + buttons | Done | Clickable chat GUI (server-safe) |
| 6 | Lightman's purchase +100/+1k/+10k | Done | Soft-dep; TP fallback |
| 7 | Admin settings / reload / `difficulty.admin` | Done | Ops + `hasPermission("difficulty.admin")` |
| 8 | State colors G/Y/O/P/R | Done | `DifficultySnapshot.stateColorCode` |
| 9 | Mob spawn scaling + DMZ extras | Done | DMZ extras only on `dragonminez:` / DMZ classes |
| 10 | Tiers 10→100000 | Done | `DifficultyTier` |
| 11 | Enemy evolution (creeper/zombie/skel/enderman/warden) | Done | `EnemyEvolution` ability stand-ins |
| 12 | Elites (name/aura/size/AI/rewards) | Done | Glow aura; size via knockback resist + NBT scale hint |
| 13 | Mutations listed | Done | + Shadow / Vampiric extras |
| 14 | Adaptive AI incl. ki-charge counter | Done | Dodge/retreat/anti-flight/focus/coord + `KiChargeEvent` |
| 15 | Rewards TP/XP/Potential/drops/capsules/titles | Done | TP event + kill package; Potential via `potentialunlock` skill progress |
| 16 | Boss scale + phases | Done | 75/50/25% phases |
| 17 | Cache / no per-tick calc | Done | Cache + 5s level/prestige refresh |

## Intentional approximations
- **GUI**: chat click GUI (no client Screen jar in Mohist toolchain)
- **Elite size / aura**: no Pehkui; knockback resist + glowing name; NBT `dmz_ad_elite_scale` for packs
- **Enemy evolution “ki beam/laser”**: effect/damage stand-ins (not full DMZ ki projectiles from vanilla mobs)
- **DMZ XP**: vanilla XP points (no dedicated DMZ XP gain event in 2.1.3)
- **Cosmetics**: capsule item drops + unlockable titles (not wardrobe skins)
