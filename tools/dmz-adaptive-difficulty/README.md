# DMZ Adaptive Difficulty (v1.6.1)

**Server-side only** Forge mixin mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

## Features

- Personal Calculated / Purchased / Active difficulty
- In-game inventory GUI via `/difficulty`:
  1. **CMI / CMILib** inventory GUI (preferred)
  2. Companion Bukkit chest GUI
  3. Clickable chat fallback
- Bukkit `/difficulty` registered by the companion plugin so **all players** can use it on Mohist
- FTB Teams teammate scaling (scoreboard fallback)
- Lightman's wallet + bank + inventory coin purchases
- Spawn mob scaling (capped), elites, mutations, enemy evolution, adaptive AI, boss phases
- **Scaling Health-inspired area difficulty**: `weighted` / `average` / `max` nearby-player modes, group bonus, spawn variance, deferred process tick
- Reward multipliers

## Install (server only)

1. Put `dmz_adaptive_difficulty-1.6.1.jar` in the **server** `mods/` folder
2. Remove older `dmz_adaptive_difficulty-*.jar` (and remove it from clients if present)
3. Ensure vanilla world difficulty is **not Peaceful** (`server.properties` → `difficulty=hard`)
4. Plugins required for the inventory GUI:
   - `CMILib*.jar` (required by CMI)
   - `CMI-*.jar`
   - `dmz_adaptive_difficulty_gui-1.6.0.jar` (**registers Bukkit `/difficulty`**)
   - Optional: `PlaceholderAPI-*.jar`
5. Optionally set `guiBackend` in `config/dmz_adaptive_difficulty.json`:
   - `cmi` (default), `auto`, `chest`, `deluxemenus`, or `chat`
6. Restart the server

## Commands

| Who | Command | Action |
|---|---|---|
| Everyone | `/difficulty` | Open CMI inventory GUI (or chest/chat fallback) |
| GUI buttons | `/difficulty do …` | Used by GUI clicks |
| Ops | `/difficulty hard\|normal\|easy\|peaceful` | Vanilla world difficulty |
| Staff | `/difficulty admin` | Toggle admin command access (shows errors if no perm) |
| Staff (toggled on) | `/difficulty admin help\|reload\|settings\|area\|gamedifficulty\|set` | Config tools |

## Area difficulty (Scaling Health example)

Mobs scale from **area difficulty** at their spawn position (like SilentChaos512 Scaling Health), not only the single nearest player:

| `areaDifficultyMode` | Behavior |
|---|---|
| `weighted` (default) | Distance-weighted average of nearby players' active difficulty |
| `average` | Simple mean of nearby players |
| `max` | Highest nearby active difficulty (old behavior) |

Also: `areaGroupBonusPercent` (extra % per extra nearby player), `areaDifficultyVariancePercent` (~±5% per mob).  
Staff diagnostic: `/difficulty admin` then `/difficulty admin area`.

## Build

```bash
bash tools/dmz-adaptive-difficulty/build.sh
bash tools/dmz-adaptive-difficulty-gui/build.sh
```
