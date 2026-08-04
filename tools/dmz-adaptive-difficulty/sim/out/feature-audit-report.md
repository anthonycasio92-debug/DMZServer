# AdaptiveDifficulty feature audit (1.0.12)

See `feature-audit-report.txt` for the full fail-closed checklist.

## Combat model (1.0.12)

- Soft offense: **STR/SKP/PWR** + mild **ENE**
- Soft HP: **VIT** + stronger offense durability sponge × `mobHealthScale` **0.90**
- Damage floors: **VIT/RES** via `tankDamageHealthRatio` / `tankDamageDefenseRatio`
- Raised VIT hit-cap budgets (T1 16% → T7 55%, base formFactor 0.70, hard ceiling 0.58)
- Class counters + top-2 combat stats (secondary @ 60%)
