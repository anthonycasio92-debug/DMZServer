# AdaptiveDifficulty feature audit (1.0.13)

See `feature-audit-report.txt` for the full fail-closed checklist.

## Combat model (1.0.13)

- Soft offense: **STR/SKP/PWR** + mild **ENE**
- Soft HP: **VIT** + stronger offense durability sponge × `mobHealthScale` **1.05**
- Damage floors: **VIT/RES** via `tankDamageHealthRatio` / `tankDamageDefenseRatio`
- Skills: reads `kiprotection`, `ki_infusion`, `potentialunlock` — infusion/PU raise pack sponge; KP is post-mitigation survival
- Raised VIT hit-cap budgets (T1 15% → T7 56%, base formFactor 0.72, hard ceiling 0.60)
- Class counters + top-2 combat stats
- Melee AI parity: Awakened+ chase, painted shock/slam, tier move bump

## Sims

- `validate_scaling.py` — formula regression
- `simulate_race_forms.py` — all races/forms
- `simulate_build_matrix.py` — race × class × archetype × skills × tier concept checks
- `audit_gui_abi.py` — Forge↔GUI reflection + version handshake
