# DMZ Adaptive Difficulty (v1.3.1)

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

Outputs `mods/dmz_adaptive_difficulty-1.3.1.jar`.

## Install

1. Put `dmz_adaptive_difficulty-1.3.1.jar` in **server and client** `mods/`
2. Remove any older `dmz_adaptive_difficulty-*.jar`
3. Ensure `lightmanscurrency` and `ftbteams` are installed on the server (optional but expected)
4. Restart

Open with `/difficulty` (standalone Screen). Chat fallback: `/difficulty chat`.

## Commands

| Command | Action |
|---|---|
| `/difficulty` / `/difficulty gui` | Open Screen GUI |
| `/difficulty chat` | Legacy clickable chat menu |
| `/difficulty up/down [n]` | Adjust active difficulty |
| `/difficulty buy <n>` | Purchase difficulty |
| `/difficulty team [mode]` | Cycle/set team mode |
| `/difficulty show` | Text snapshot |

## Notes

- Purchase costs use `baseCost` / `costScaling` from `config/dmz_adaptive_difficulty.json` (default base 100000 core coin value).
- Balance shown in GUI is combined wallet + bank + inventory.
- Team scaling uses FTB party/server teams; solo personal teams count as no teammates.
