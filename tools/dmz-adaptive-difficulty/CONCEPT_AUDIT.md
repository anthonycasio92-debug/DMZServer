# Concept audit vs `DragonMineZ_Adaptive_Difficulty_System_Concept_019c.txt`

Mod version: **1.8.3** · GUI: **1.8.3**  
Source concept: `tools/dmz-adaptive-difficulty/DragonMineZ_Adaptive_Difficulty_System_Concept_019c.txt`

| § | Concept | Status | Implementation |
|---|---------|--------|----------------|
| 1 | System overview | Done | Personal / calculated / purchased / active + team + scaling stack |
| 2 | Optional + Lightman's + team | Done | Active selection, inventory coin purchases, team modes |
| 3 | Five difficulty values | Done | `DifficultyCalculator` + `PlayerDifficultyData` + `TeamScaling` |
| 4 | Personal / Threshold / Full team | Done | `TeamMode` + FTB Teams (scoreboard fallback); +10%/teammate default |
| 5 | `/difficulty` GUI fields + buttons | Done | Hub shows Active/Max/Calculated/Purchased/**Team bonus/contrib**; Adjust / Team / Rewards / Tiers / Titles / Details |
| 6 | Purchase difficulty | Done* | Iron-coin trapezoid cost (configurable); lowering free; Buy Max page |
| 7 | Admin config | Done* | `/difficulty admin` toggle + `set` / `reload` / `settings` / tier keys |
| 8 | State colors G/Y/O/P/R | Done | `DifficultySnapshot.state()` |
| 9 | Mob scaling | Done* | Spawn area scale + per-player retarget; power curves (not literal +1%/level); DMZ extras on hostiles |
| 10 | Tiers 10→100000 | Done+ | Extended Awakened→Zenith (10M) |
| 11 | Enemy evolution kits | Done | Creeper (incl. **tracking fuse chase**), Zombie, Skeleton ki, Enderman, Warden + extras |
| 12 | Elites | Done* | Name, glow aura, KB-resist “size”, AI floor, reward bonus (no Pehkui) |
| 13 | Mutations (5 listed) | Done+ | All five + Shadow / Vampiric |
| 14 | Adaptive AI | Done | Dodge / retreat / ki-charge / anti-flight / focus weakest / pack |
| 15 | Reward scaling | Done* | `1 + diff / rewardScaling` → **XP + drop odds**; capsules; titles. **TP & Potential not touched** (by design) |
| 16 | Boss scaling + phases | Done* | Inherit difficulty; HP/dmg/def; 75/50/25% phase bursts |
| 17 | Performance / cache | Done | **No LivingTick / no combat AABB**; `CombatIndex` + `BehaviorScheduler`; difficulty/area/retarget caches; curve LUT |

\* = intentional approximation (still fulfills the gameplay intent).

## Intentional approximations
- **GUI host**: CMI / chest / chat (no Forge client Screen)
- **Elite size**: knockback resist + NBT scale hint (no Pehkui)
- **DMZ XP**: vanilla XP points scaled by reward multiplier (no dedicated DMZ XP event)
- **TP / Potential**: never granted or multiplied — TP stays with DragonMineZ; Potential with CNPC `Potential.js`
- **Purchase formula**: iron-coin ramp (`baseCostIronCoins` × scale) instead of legacy `baseCost × purchased/costScaling`
- **Phys vs Ki extras**: folded into shared outgoing damage multiplier

## 1.8.3 — The End isolation
- Config `disabledDimensions` defaults to `minecraft:the_end` (skip scale/AI/evo/gravity)
- CNPC `End Dimension Strength.js` **2.10.4**: no tick-path `getAllEntities`; world scan lock; smaller dragon AABB; no force-load on fight lookup

## 1.8.2 follow-up
- CombatIndex keeps weak refs (no all-level UUID scan every pulse)
- Combat gravity ticks only players with contributions
- LivingHurtEvent fast-rejects non-player/non-hostile damage
- Spark Forge jar + `tools/SPARK.md` for TPS isolation

## 1.8.1 TPS fix (combat index)
- Removed remaining `getEntitiesOfClass` / AABB combat scans (pack call, ki-charge, piglin swarm, solar/slam player fan-out)
- Engaged mobs registered from target-change + hurt via `tick.CombatIndex` (UUID TTL)
- `BehaviorScheduler` pulses only the combat index (budget 48 / 20t) — never neighborhood Mob AABB
- Dropped `LivingAttackEvent` retarget (every swing); retarget stays on ChangeTarget + Hurt (cached)
- Area difficulty always uses online player-list distance (never player AABB)

## 1.8.0 rewrite notes (optimization + concept close-out)
- Replaced per-entity `LivingTickEvent` with `tick.BehaviorScheduler`
- Combat gravity batched on the server pulse
- Curve LUT for offense/health (`ScalingCurves`)
- Soft-prune retarget difficulty cache; area cache not cleared on every raise/buy
- Concept §15 reward multiplier restored for XP/drops only
- Concept §11 creeper **tracking explosion**
- Hub GUI shows team bonus + contribution (§5)

## Install
1. `mods/dmz_adaptive_difficulty-1.8.3.jar`
2. `plugins/dmz_adaptive_difficulty_gui-1.8.3.jar`
3. Optional: `mods/spark-1.10.53-forge.jar` (`tools/SPARK.md`)
4. Reload CNPC **End Dimension Strength** 2.10.4
