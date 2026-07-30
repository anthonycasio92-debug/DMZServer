# DMZ Adaptive Difficulty (v1.2.0)

Server-side Forge/Mohist 1.20.1 mod implementing the Adaptive Difficulty concept.

## Features

### Phase 1
- Personal **Calculated / Purchased / Active** difficulty
- Prestige-aware calculated difficulty (`prestige` DMZ skill × multiplier)
- Team modes via vanilla scoreboard teams
- Mob scaling once at spawn
- TP reward multiplier via `DMZEvent.TPGainEvent`

### Phase 2 / concept-complete (1.2.0)
- **Lightman's Currency** purchases (`MoneyAPI` / `CoinValue.fromNumber("main", cost)`)
- Clickable **difficulty GUI** (`/difficulty`) — up/down/buy/team + Rewards/Tiers/Stats/Settings
- **Enemy evolution** (creeper/zombie/skeleton/enderman/warden abilities by tier)
- **Elite** spawns (glowing named elites, size stand-in, higher stats/rewards)
- **Mutations** (Burning, Electric, Gravity, Titan, Berserker, Shadow, Vampiric)
- **Adaptive AI** (focus weakest, dodge, retreat, anti-flight, coordinated, ki-charge counter)
- **Boss scaling** + HP phases (75% / 50% / 25%)
- **Rewards**: TP, XP, Potential progress, rare drops, capsules, titles

See `CONCEPT_AUDIT.md` for the section-by-section checklist.

## Build

```bash
bash tools/dmz-adaptive-difficulty/build.sh
```

Requires `libraries/lightmanscurrency-1.20.1-2.3.0.5.jar` (or same file under `mods/`) on the compile classpath.

Outputs `mods/dmz_adaptive_difficulty-1.2.0.jar`.

## Install

1. Put `dmz_adaptive_difficulty-1.2.0.jar` in `mods/`
2. Put `lightmanscurrency-1.20.1-2.3.0.5.jar` in `mods/` (optional but recommended)
3. Restart → config at `config/dmz_adaptive_difficulty.json`

If Lightman's is missing, purchases automatically fall back to Training Points.

## Commands

| Command | Description |
|---|---|
| `/difficulty` | Open clickable GUI |
| `/difficulty gui [main\|rewards\|tiers\|stats]` | Open a GUI page |
| `/difficulty show` | Plain text snapshot |
| `/difficulty up\|down [n]` | Change **active** difficulty (free) |
| `/difficulty set <n>` | Set active within available max |
| `/difficulty buy <amount>` | Buy purchased difficulty (Lightman's / TP) |
| `/difficulty team [personal\|threshold\|full]` | Team scaling mode |
| `/difficulty reload` | Reload config (op) |
| `/difficulty admin set <key> <value>` | Edit config (op) |

## Config highlights

- `purchaseCurrency`: `lightmans` (default) \| `training_points` \| `free`
- `eliteChancePercent`, `mutationChancePercent`
- `enableAdaptiveAi`, `enableBossScaling`, `bossHealthThreshold`
- Scaling percents + reward/cost formulas from the concept doc
