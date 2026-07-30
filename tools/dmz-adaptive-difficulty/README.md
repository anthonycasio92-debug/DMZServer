# DMZ Adaptive Difficulty (Phase 1)

Server-side Forge/Mohist 1.20.1 mod implementing the Adaptive Difficulty concept:

- Personal **Calculated / Purchased / Active** difficulty
- Prestige-aware calculated difficulty (`prestige` DMZ skill × multiplier)
- Team modes via vanilla scoreboard teams
- Mob scaling once at spawn (cached on entity NBT)
- Training Points reward multiplier via `DMZEvent.TPGainEvent`
- Admin JSON config + `/difficulty` commands

## Build

```bash
bash tools/dmz-adaptive-difficulty/build.sh
```

Outputs `mods/dmz_adaptive_difficulty-1.0.0.jar`.

## Commands

| Command | Description |
|---|---|
| `/difficulty` | Show personal + team difficulty snapshot |
| `/difficulty up [n]` / `down [n]` | Change **active** difficulty (free) |
| `/difficulty set <n>` | Set active difficulty within available max |
| `/difficulty buy <amount>` | Permanently buy purchased difficulty (TP by default) |
| `/difficulty team [personal\|threshold\|full]` | Cycle/set team scaling mode |
| `/difficulty reload` | Reload config (op) |
| `/difficulty admin set <key> <value>` | Edit config keys (op) |

## Config

`config/dmz_adaptive_difficulty.json`

Important keys:

- `prestigeMultiplier` (default 10)
- `teamBonusPercent` / `contributionPercent`
- `baseCost` / `costScaling`
- `rewardScaling`
- `healthPercentPerDifficulty` / `damagePercentPerDifficulty` / `defensePercentPerDifficulty`
- `purchaseCurrency`: `training_points` (default), `free`, or `lightmans` (stub)

## Phase 1 vs later

**Included:** personal difficulty, team threshold/contribution, spawn scaling, TP reward mult, commands, config.

**Deferred:** Lightman's Currency wire-up, difficulty GUI, elite/mutations, adaptive AI, boss phases, DMZ XP/drop tables beyond TP.

## Notes

- Prestige is read from DMZ skill `"prestige"` (same source your Prestige Sync Fabled scripts maintain).
- Purchases default to DMZ Training Points until Lightman's is installed and wired.
- Compile uses SRG Minecraft names (same pattern as `dmz_mohist_melee_fix`).
