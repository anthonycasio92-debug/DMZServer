# DMZ Adaptive Difficulty (v1.8.3)

**Server-side only** Forge mixin mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

## Features

- Personal Calculated / Purchased / Active difficulty
- In-game inventory GUI via `/difficulty`:
  1. **CMI / CMILib** inventory GUI (preferred)
  2. Companion Bukkit chest GUI
  3. Clickable chat fallback
- Choosable steps on a dedicated **Adjust** page: **+1 / +5 / +25 / +100 / +1000 / +10000 / +100000** (and matching lowers)
- Separate **Buy Max** page for unlocking more available max
- Bukkit `/difficulty` registered by the companion plugin so **all players** can use it on Mohist
- FTB Teams teammate scaling (scoreboard fallback)
- Raising difficulty always costs **Lightman's iron coins** (scaled); lowering/reset is free
- Payments take coins from the **player inventory only** (not wallet/bank)
- **Per-player retarget scaling**: mobs capture base HP/damage at spawn, then re-scale to the engaged player's difficulty on target switch / hit (HP % preserved)
- Elites, mutations, **per-mob evolution kits**, adaptive AI, boss phases
- Kill rewards: XP + drop odds use concept `1 + difficulty / rewardScaling`; **no TP / Potential** writes
- Titles catalog + equip GUI; capsules on elite/boss/mut kills
- Scaling Health-inspired area difficulty for initial spawn (`weighted` / `average` / `max`)
- **1.8.3**: skips The End by default (`disabledDimensions`) — End Strength script owns that dim; End script 2.10.4 TPS cuts

## Install (server only)

1. Put `dmz_adaptive_difficulty-1.8.3.jar` in the **server** `mods/` folder
2. Remove older `dmz_adaptive_difficulty-*.jar` (and remove it from clients if present)
3. Ensure vanilla world difficulty is **not Peaceful** (`server.properties` → `difficulty=hard`)
4. Plugins required for the inventory GUI:
   - `CMILib*.jar` (required by CMI)
   - `CMI-*.jar`
   - `dmz_adaptive_difficulty_gui-1.8.3.jar` (**registers Bukkit `/difficulty`**)
5. Optional profiler: `mods/spark-1.10.53-forge.jar` — see `tools/SPARK.md`
6. Reload CNPC player script **End Dimension Strength** (v2.10.4) or restart
   - Optional: `PlaceholderAPI-*.jar`
5. Optionally set `guiBackend` in `config/dmz_adaptive_difficulty.json`:
   - `cmi` (default), `auto`, `chest`, or `chat`
6. Restart the server

> DeluxeMenus is **not** used. Remove any old `DeluxeMenus` DMZ difficulty menus if present.

## Max difficulty (no artificial hardcap)

**Theoretical max** comes from DMZ stats:

- Prestige 0: `level × levelMultiplier`
- Prestige 1+: `level × levelMultiplier × (prestige × prestigeMultiplier)` (default prestige ×10)

At DMZ **level 100000** with **10 prestiges** (defaults): **10,000,000** theoretical max.

Available max = theoretical + purchased + optional team bonuses.  
`hardCapDifficulty` defaults to **0** (disabled).

## Difficulty tiers

| Tier | Threshold |
|---|---|
| Awakened → Impossible | 10 → 100,000 |
| Transcendent | 250,000 |
| Eternal | 500,000 |
| Mythic | 1,000,000 |
| Omega | 2,500,000 |
| Absolute | 5,000,000 |
| Apex | 7,500,000 |
| **Zenith** | **10,000,000** (P10 max-level ceiling) |

## Commands

| Who | Command | Action |
|---|---|---|
| Everyone | `/difficulty` | Open CMI inventory GUI (or chest/chat fallback) |
| Everyone | `/difficulty reset` | Set **active** difficulty to 0 (free; purchased max kept) |
| GUI | **Reset to 0** button | Same as `/difficulty reset` |
| GUI buttons | `/difficulty do …` | Used by GUI clicks |
| Ops | `/difficulty hard\|normal\|easy\|peaceful` | Vanilla world difficulty |
| Staff | `/difficulty admin` | Toggle admin command access (shows errors if no perm) |
| Staff (toggled on) | `/difficulty admin help\|reload\|settings\|area\|resetpurchased\|gamedifficulty\|set` | Config tools |

## Combat & reward curves

**Retarget scaling:** after spawn, base HP/damage/armor/speed are stored. When a player is hit by / hits / is targeted by a mob, stats rewrite to that player's active difficulty (current HP % kept). Multiple players can share a fight without the mob staying locked to the wrong power level.

**Endgame anchors (level ~100k / ~8M active):** player HP ≈ **800k**, DEF ≈ **200k**.
Offense rates keep a normal melee hit under ~800k raw and mob armor ≈ **200k**.

| Stat | Curve | Defaults |
|---|---|---|
| Damage / defense | Steep offense curve (**uncapped**) | exp `0.96`, pivot `450`, rates `0.62`/`1.85` |
| Health | Flat health curve | exp `0.40`, mult uncapped, abs cap **1024** HP |
| Kill rewards | XP / drops × `(1 + d / rewardScaling)` | Cap ×25; TP never touched |
| Train TP / Potential | **Not touched** | `TPGainEvent` pass-through; Potential = CNPC |
| AI | Spaced across Awakened→Zenith | anti-flight Legendary+; pack/debuffs scale to Zenith |

Tune:
```
/difficulty admin set combatCurveExponent 0.96
/difficulty admin set combatCurvePivot 450
/difficulty admin set damagePercentPerDifficulty 0.62
/difficulty admin set defensePercentPerDifficulty 1.85
/difficulty admin set maxDamageMultiplier 0
/difficulty admin set maxArmorBonus 0
/difficulty admin set healthCurveExponent 0.40
/difficulty admin set maxHealthMultiplier 0
/difficulty admin set maxScaledHealth 1024
```
(`maxDamageMultiplier` / `maxHealthMultiplier` `0`/`1` = uncapped; `maxArmorBonus` `0` = uncapped.)

## Pricing (Lightman's iron coins)

Raising active difficulty and buying more max **always** costs Lightman's Currency from **inventory coins only**.
Higher coins (gold, emerald, …) are accepted for iron-priced costs — leftover is returned as change.

Default formula (iron coins):

```
cost ≈ amount × baseCostIronCoins × (1 + costScalePerDifficulty × (from + (amount-1)/2))
```

Defaults:
- `baseCostIronCoins = 1` → first levels cost ~1 iron coin each
- `costScalePerDifficulty = 0.01` → at difficulty 100, each +1 costs ~2 iron
- `costCoinItem = lightmanscurrency:coin_iron`

Tune live:
```
/difficulty admin set baseCostIronCoins 1
/difficulty admin set costScalePerDifficulty 0.01
/difficulty admin set costCoinItem lightmanscurrency:coin_iron
```

Lowering and Reset are free. No refunds.

## Area difficulty

| `areaDifficultyMode` | Behavior |
|---|---|
| `weighted` (default) | Distance-weighted average of nearby players' active difficulty |
| `average` | Simple mean of nearby players |
| `max` | Highest nearby active difficulty |

Also: `areaGroupBonusPercent`, `areaDifficultyVariancePercent`.  
Staff diagnostic: `/difficulty admin` then `/difficulty admin area`.

## Build

```bash
bash tools/dmz-adaptive-difficulty/build.sh
bash tools/dmz-adaptive-difficulty-gui/build.sh
```
