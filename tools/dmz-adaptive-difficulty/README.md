# DMZ Adaptive Difficulty (v1.7.1)

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
- Scaling Health-inspired area difficulty (`weighted` / `average` / `max`)
- Optimized tick path: unmarked mobs exit immediately; AI/evolution staggered

## Install (server only)

1. Put `dmz_adaptive_difficulty-1.7.1.jar` in the **server** `mods/` folder
2. Remove older `dmz_adaptive_difficulty-*.jar` (and remove it from clients if present)
3. Ensure vanilla world difficulty is **not Peaceful** (`server.properties` → `difficulty=hard`)
4. Plugins required for the inventory GUI:
   - `CMILib*.jar` (required by CMI)
   - `CMI-*.jar`
   - `dmz_adaptive_difficulty_gui-1.7.1.jar` (**registers Bukkit `/difficulty`**)
   - Optional: `PlaceholderAPI-*.jar`
5. Optionally set `guiBackend` in `config/dmz_adaptive_difficulty.json`:
   - `cmi` (default), `auto`, `chest`, or `chat`
6. Restart the server

> DeluxeMenus is **not** used. Remove any old `DeluxeMenus` DMZ difficulty menus if present.

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
