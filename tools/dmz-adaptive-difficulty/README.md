# DMZ Adaptive Difficulty (v1.5.0)

**Server-side only** Forge mixin mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

## Features

- Personal Calculated / Purchased / Active difficulty
- In-game inventory GUI via `/difficulty`:
  1. **DeluxeMenus** (preferred when installed)
  2. Companion Bukkit **chest GUI** plugin
  3. Clickable chat fallback
- FTB Teams teammate scaling (scoreboard fallback)
- Lightman's wallet + bank + inventory coin purchases
- Spawn mob scaling (capped), elites, mutations, enemy evolution, adaptive AI, boss phases
- Reward multipliers

## Install (server only)

1. Put `dmz_adaptive_difficulty-1.5.0.jar` in the **server** `mods/` folder
2. Remove older `dmz_adaptive_difficulty-*.jar` (and remove it from clients if present)
3. For inventory GUI (recommended on Mohist), keep these in `plugins/`:
   - `DeluxeMenus-*.jar` + `plugins/DeluxeMenus/` menus (shipped)
   - `PlaceholderAPI-*.jar`
   - `dmz_adaptive_difficulty_gui-1.5.0.jar` (PAPI placeholders + chest fallback)
4. Optionally set `guiBackend` in `config/dmz_adaptive_difficulty.json`:
   - `auto` (default), `deluxemenus`, `chest`, or `chat`
5. Restart the server

## Commands

| Who | Command | Action |
|---|---|---|
| Everyone | `/difficulty` | Open inventory GUI (or chat fallback) |
| GUI buttons | `/difficulty do …` | Used by GUI clicks (not for normal typing) |
| Staff | `/difficulty admin` | Toggle admin command access |
| Staff (toggled on) | `/difficulty admin help\|reload\|settings\|set` | Config tools |

## Build

```bash
bash tools/dmz-adaptive-difficulty/build.sh
bash tools/dmz-adaptive-difficulty-gui/build.sh
```
