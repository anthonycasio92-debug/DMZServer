# AdaptiveDifficulty feature audit (1.0.10)

Run: `python3 tools/dmz-adaptive-difficulty/sim/audit_features.py`

## Scorecard

| Feature | Status |
|---------|--------|
| Tiers 1–7 via Ancient Coins | OK |
| Live gate: DMZ level **or** Prestige ≥ tier id | OK |
| Prestige/level revoke (reliable sample) | OK |
| Nearby scale × tier%; max 5/player | OK |
| Mob damage: soft STR/SKP + VIT hit cap | OK |
| Mob HP: soft VIT + floors/caps × `mobHealthScale` | OK |
| PWR/ENE never scaled against | OK |
| Tank/RES: no ATK pierce inflation | OK |
| Form soft-curve / mega / addition peel | OK |
| Exempt: saga/quest, SDD, SPAWNER, dragon, slime split | OK |
| Stock ladder 21/42/65/90/135/160/200%; form 0.55/0.75; HP 0.65 | OK |
| Personal OFF freezes scale/coins/AI/buy | OK |
| Death clears active tier; logout keeps | OK |
| Coin chat mute + kill Ancient Coins | OK |
| Gates T1 AI/evo · T4 elite · T5 mutation · T6 boss | OK |
| Titles equip; teams WIP stub | OK |
| Admin enabled + whitelist | OK |
| Future-race CombatSanity / baselines | OK |
| GUI ABI package + version handshake | OK (`audit_gui_abi.py`) |

## Corrections applied in this audit

- `tankDamageDefenseRatio` / `tankDamageHealthRatio` marked **unused** (1.0.8+); blocked from live admin/GUI set (were admin foot-guns with no combat effect).
- Tank/paladin class counter no longer adds residual mob ATK (~2% stock) — RES + ki protection remain the counters.
- GUI `mobHealthScale` invalid fallback aligned to **0.65** (was 0.5).
