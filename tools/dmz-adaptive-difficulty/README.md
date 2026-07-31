# DMZ Adaptive Difficulty (v1.7.31)

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
- Spawn mob scaling for **all hostiles** with the same DMZ-style extras (HP/DEF/DMG/ki), elites, mutations, **per-mob evolution kits**, adaptive AI, boss phases
- Scaling Health-inspired area difficulty (`weighted` / `average` / `max`)
- Optimized tick path: unmarked mobs exit immediately; AI/evolution staggered

## Install (server only)

1. Put `dmz_adaptive_difficulty-1.7.31.jar` in the **server** `mods/` folder
2. Remove older `dmz_adaptive_difficulty-*.jar` (and remove it from clients if present)
3. Ensure vanilla world difficulty is **not Peaceful** (`server.properties` → `difficulty=hard`)
4. Plugins required for the inventory GUI:
   - `CMILib*.jar` (required by CMI)
   - `CMI-*.jar`
   - `dmz_adaptive_difficulty_gui-1.7.12.jar` (**registers Bukkit `/difficulty`**)
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

**Endgame anchors (level ~100k / ~8M active):** player HP ≈ **800k**, DEF ≈ **200k**.
Offense rates keep a normal melee hit under ~800k raw (tankable from HP alone) and mob armor ≈ **200k**.

Curves are **split**:

| Stat | Curve | Defaults |
|---|---|---|
| Damage / defense | Steep offense curve (**uncapped**) | exp `0.96`, pivot `450`, rates `0.62`/`1.85`; anchored to ~800k HP / ~200k DEF at 8M |
| Health | Flat health curve + hard caps | exp `0.40`, max mult `×8`, abs cap `400` HP |
| Kill TP | Absolute difficulty curve | `1000000 × (d / 10000000)^0.70` → **~1M at Zenith**; hard cap **10M** |
| Train TP mult | Mild power mult on DMZ `TPGainEvent` | gain `0.85`, exp `0.38`, scale `2500`, mult cap **×10**, grant cap **10M** |
| Potential unlock | **Not touched** | Owned by CNPC `Potential.js` (`potentialunlock`) |
| AI | Spaced across Awakened→Zenith | anti-flight Legendary+; pack/debuffs scale to Zenith |

| Active | Kill TP (normal) | Peak (elite+boss+mut) |
|---|---|---|
| 1,000 | ~1,200 | ~9,000 |
| 10,000 | ~6,000 | ~45,000 |
| 100,000 | ~30,000 | ~224,000 |
| 1,000,000 | ~150,000 | ~1.1M |
| 8,000,000 | ~642,000 | ~4.8M |
| **10,000,000** | **~1,000,000** | **~7.5M** (capped at 10M) |

(Elites ×`eliteRewardBonus`, bosses ×3, mutations ×1.25. Single-grant ceiling: `maxKillTp` / `maxTpGainEvent` = **10M**.)

Tune:
```
/difficulty admin set combatCurveExponent 0.96
/difficulty admin set combatCurvePivot 450
/difficulty admin set damagePercentPerDifficulty 0.62
/difficulty admin set defensePercentPerDifficulty 1.85
/difficulty admin set maxDamageMultiplier 0
/difficulty admin set maxArmorBonus 0
/difficulty admin set healthCurveExponent 0.40
/difficulty admin set maxHealthMultiplier 8
/difficulty admin set maxScaledHealth 400
/difficulty admin set killTpRefAmount 1000000
/difficulty admin set killTpRefDifficulty 10000000
/difficulty admin set killTpExponent 0.70
/difficulty admin set maxKillTp 10000000
/difficulty admin set maxTpGainEvent 10000000
/difficulty admin set maxRewardMultiplier 10
```
(`maxDamageMultiplier` `0`/`1` = uncapped; `maxArmorBonus` `0` = uncapped; `maxKillTp` / `maxTpGainEvent` `0` = uncapped.)

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
