# Legacy Mechanics 2.0.0

Forge mod packaging **Difficulty**, **Rival System 4.7.10**, **Sparring TP 3.2.11**,
and **natural progression / Fabled bridges** (from CNPC scripts) under one product.
Java package stays `com.dbzlegacy.adaptivedifficulty` and NBT root
`dmz_adaptive_difficulty` for GUI ABI.

## Install (test)

1. Build / install `LegacyMechanics-2.0.0.jar` (+ matching `LegacyMechanicsGUI-2.0.0.jar`).
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

## Player GUIs (CMI → chest → chat)

Bare player commands open **inventory GUIs** (same as Difficulty). Prefer clicking;
typed commands are for admin / edge cases.

| Command | GUI |
|---------|-----|
| `/lm` | Hub — Difficulty · Rival · Spar · Progression · Prestige · Skills |
| `/difficulty` | Unlock tiers · Titles · personal toggle |
| `/rival` | List / Challenge / pickers (declare · accept · remove · challenge · spectate) |
| `/spar` | Stats · Top · Mentor pickers (invite apprentice / ask mentor) |
| `/progression` | Status · Prestige · Skills · (staff Flags) |
| `/prestige` | Prestige confirm |
| `/skills` | Core / Advanced / Saga |

## Admin commands (keep typed)

- `/difficulty admin …` — whitelist, syslog, inspect GUI, reload, settings
- `/progression boost start|end`, `meditation next`, `android` (staff)
- `/rival refresh|save`, `/spar admin …|save` (staff)
- `/enddragon`, `/cleardragons`

### Rival features

Presence TP, surpass, challenges + battle reports, instinct, Proving Grounds,
kill-near / underdog / anti-gank (offense aura stays off), level TP curve,
seasons / quests / achievements / HOF / journal / titles, spectator, fusion kill share.

### Sparring features

Auto sessions, mentor, block TP, clash drip, Friendly Fist, release-control drip,
perfect training banner, ki type classification, charging holds activity, `/spar top`.

### Progression / Fabled

See **[PROGRESSION.md](PROGRESSION.md)** for module map, config toggles, and CNPC scripts
to disable on test servers. Full script/branch coverage: **[AUDIT.md](AUDIT.md)**.

Also includes Building TP (place), End portal guard, and Title progression / Admin
inspect (from the server-fixes line).

## Telemetry

- Balance hits (whitelist): `hits-YYYY-MM-DD.jsonl`
- System events (all players, rate-limited): `systems-YYYY-MM-DD.jsonl`
  (includes `system=fabled` bridge sync events)
