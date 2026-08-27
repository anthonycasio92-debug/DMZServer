# Legacy Mechanics 1.0.46

Forge mod packaging **Difficulty**, **Rival System 4.7.10**, **Sparring TP 3.2.11**,
and **natural progression / Fabled bridges** (from CNPC scripts) under one product.
Java package stays `com.dbzlegacy.adaptivedifficulty` and NBT root
`dmz_adaptive_difficulty` for GUI ABI.

## Install (test)

1. Build / install `LegacyMechanics-1.0.46.jar` (+ matching `AdaptiveDifficultyGUI-1.0.46.jar`).
2. Disable CNPC Global Player scripts to avoid double systems — see **[PROGRESSION.md](PROGRESSION.md)**
   for the full disable list (Fabled bridges + natural ports + rival/spar).
3. Do **not** deploy to live until tested.

## Data

| System | Path |
|--------|------|
| Config | `config/legacymechanics.json` (migrates from `adaptivedifficulty.json` / `dmz_adaptive_difficulty.json`) |
| Rivalry | `config/legacymechanics/rivalry-v4.json` |
| Progression | `config/legacymechanics/progression-v4.json` |
| Sparring | `config/legacymechanics/sparring.json` |
| Telemetry | `config/legacymechanics/telemetry/` |

Flags: `enableRivalSystem`, `enableSparringSystem`, `rivalPresenceTp`, `rivalInstinct`,
`rivalChallenges`, `enableProgression`, `enableFabledBridge`, `enableSystemTelemetry`,
`balanceTelemetryEnabled` (+ per-module progression/Fabled toggles).

## Commands

- `/lm` / `/legacymechanics` — hub (Difficulty / Rival / Sparring / Progression / Logs / Help)
- `/lm do page progression` — progression + Fabled bridge status; `fabled` / `disable` pages
- `/difficulty` — Unlock tiers (chat title: Legacy Mechanics · Difficulty)
- `/rival` / `/rival gui` — Rival chat GUI; `/rival do …`
- `/spar` / `/spar gui` — Spar chat GUI; `/spar do …`
- `/difficulty admin syslog on|off|status|flush` — unified system log

### Rival features

Presence TP, surpass, challenges + battle reports, instinct, Proving Grounds,
kill-near / underdog / anti-gank (offense aura stays off), level TP curve,
seasons / quests / achievements / HOF / journal / titles, spectator, fusion kill share.

### Sparring features

Auto sessions, mentor, block TP, clash drip, Friendly Fist, release-control drip,
perfect training banner, ki type classification, charging holds activity, `/spar top`.

### Progression / Fabled

See **[PROGRESSION.md](PROGRESSION.md)** for module map, config toggles, and CNPC scripts
to disable on test servers.

## Telemetry

- Balance hits (whitelist): `hits-YYYY-MM-DD.jsonl`
- System events (all players, rate-limited): `systems-YYYY-MM-DD.jsonl`
  (includes `system=fabled` bridge sync events)
