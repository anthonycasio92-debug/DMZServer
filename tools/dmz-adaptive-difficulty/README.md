# DMZ Adaptive Difficulty (v1.3.3)

Server + client Forge mixin mod for Mohist/Forge 1.20.1.

## What's new in 1.3.x

- **Standalone Screen GUI** — `/difficulty` opens its own panel (not in the SDU hub)
- **FTB Teams** for teammate detection (falls back to scoreboard if FTB missing)
- **Lightman's fix** — checks/charges **wallet + bank + inventory coins** (was wallet-only)

## Features

- Personal Calculated / Purchased / Active difficulty
- Prestige via DMZ skill `"prestige"`
- Team modes: personal / threshold / full
- Lightman's purchases (Training Points / free fallbacks)
- Spawn mob scaling, elites, mutations, enemy evolution, adaptive AI, boss phases
- Reward multipliers (TP, XP, capsules, rare drops, titles)

## Build

```bash
bash tools/dmz-adaptive-difficulty/build.sh
```

Outputs `mods/dmz_adaptive_difficulty-1.3.3.jar`.

## Install

1. Put `dmz_adaptive_difficulty-1.3.3.jar` in **server and client** `mods/`
2. Remove any older `dmz_adaptive_difficulty-*.jar`
3. If `config/dmz_adaptive_difficulty.json` already exists, delete it (or add the new cap fields) so spawn caps apply
4. Ensure `lightmanscurrency` and `ftbteams` are installed on the server (optional but expected)
5. Restart

## Commands

| Who | Command | Action |
|---|---|---|
| Everyone | `/difficulty` | Open the Screen GUI (only player command) |
| Staff | `/difficulty admin` | Toggle admin command access on/off |
| Staff (toggled on) | `/difficulty admin help\|reload\|settings` | Admin tools |
| Staff (toggled on) | `/difficulty admin set <key> <value>` | Change config |

Gameplay (up/down/buy/team) is **GUI-only** — no chat shortcuts for players.

## Notes

- Purchase costs use `baseCost` / `costScaling` from `config/dmz_adaptive_difficulty.json` (default base 100000 core coin value).
- Balance shown in GUI is combined wallet + bank + inventory.
- Team scaling uses FTB party/server teams; solo personal teams count as no teammates.
