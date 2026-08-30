# RaceClass Fabled skill sync

When `enableRaceClassSync` is on, LegacyMechanics:

1. Sets the player's Fabled **class** to match their DMZ race (`RaceClassSync` — same as legacy `Races.js`).
2. **Discovers every race** on the server from:
   - `config/dragonminez/races/*` directories
   - Fabled classes in group `race`
3. **Ensures a Fabled Race skill** exists for each (creates `plugins/Fabled/dynamic/skill/<Name>.yml` + registers at runtime when missing).
4. **Auto-grants** that skill at level ≥ 1 while the player is that race.

## Purchase gates (not auto-granted)

Skills listed as `fabledSkill` in `config/legacymechanics/race-lock.json` (Ancient Saiyan, Sento Saiyan by default) are **never** auto-granted — players still buy them from the Prestige tree. Race Lock keeps enforcing those.

## Toggle

Same flag as class sync: `enableRaceClassSync` in `config/legacymechanics.json` (or `/prog` Fabled page).

## Telemetry

- `race_class` — class changed
- `race_skill` — skill granted
- `race_skill_miss` / `race_skill_reject` — skill missing or grant failed
