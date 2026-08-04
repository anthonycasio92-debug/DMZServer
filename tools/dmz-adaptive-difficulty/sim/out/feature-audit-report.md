# AdaptiveDifficulty feature audit (1.0.11)

Run: `python3 tools/dmz-adaptive-difficulty/sim/audit_features.py`

## Combat model (1.0.11)

- Soft offense: **STR/SKP/PWR** + mild **ENE** pool (`×0.08`) × tier%
- Soft HP: **VIT** + mild offense durability floor × `mobHealthScale`
- Counters: **fighting class** + **top-2** of STR/SKP/RES/VIT/PWR/ENE (tier-ramped)
- VIT-relative hit cap retained (ki-protection friendly)

## Scorecard

| Feature | Status |
|---------|--------|
| Tiers / coins / level∨prestige gate | OK |
| Nearby scale max 5 / stock ladder | OK |
| PWR/ENE in offense + form peak | OK |
| Class counters + top-2 stats | OK |
| Exemptions / personal / death / coins | OK |
| Future-race CombatSanity + GUI ABI | OK |
