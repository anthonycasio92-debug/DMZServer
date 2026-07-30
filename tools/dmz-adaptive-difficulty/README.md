# DMZ Adaptive Difficulty (v1.4.0)

**Server-side only** Forge mixin mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

## Features

- Personal Calculated / Purchased / Active difficulty
- Clickable chat GUI via `/difficulty` (no client Screen / no network channel)
- FTB Teams teammate scaling (scoreboard fallback)
- Lightman's wallet + bank + inventory coin purchases
- Spawn mob scaling (capped), elites, mutations, enemy evolution, adaptive AI, boss phases
- Reward multipliers

## Install (server only)

1. Put `dmz_adaptive_difficulty-1.4.0.jar` in the **server** `mods/` folder
2. Remove older `dmz_adaptive_difficulty-*.jar` (and remove it from clients if present)
3. Optionally reset `config/dmz_adaptive_difficulty.json`
4. Restart the server

## Commands

| Who | Command | Action |
|---|---|---|
| Everyone | `/difficulty` | Open clickable chat GUI |
| GUI buttons | `/difficulty do …` | Used by chat clicks (not for normal typing) |
| Staff | `/difficulty admin` | Toggle admin command access |
| Staff (toggled on) | `/difficulty admin help\|reload\|settings\|set` | Config tools |

## Build

```bash
bash tools/dmz-adaptive-difficulty/build.sh
```
